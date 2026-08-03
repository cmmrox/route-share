package com.routeshare.support.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.support.dto.CreateTicketRequest;
import com.routeshare.support.dto.SupportMessageResponse;
import com.routeshare.support.dto.SupportTicketResponse;
import com.routeshare.support.dto.TicketMessageRequest;
import com.routeshare.support.entity.SupportAttachmentEntity;
import com.routeshare.support.entity.SupportMessageEntity;
import com.routeshare.support.entity.SupportTicketEntity;
import com.routeshare.support.repository.SupportMessageRepository;
import com.routeshare.support.repository.SupportTicketRepository;
import com.routeshare.support.service.SupportService;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportServiceImpl implements SupportService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final SupportTicketRepository tickets;
  private final SupportMessageRepository messages;
  private final com.routeshare.support.repository.SupportAttachmentRepository attachments;
  private final com.routeshare.storage.service.ObjectStoragePort storage;
  private final com.routeshare.storage.config.ObjectStorageProperties storageProperties;
  private final java.time.Clock clock;
  private final long attachmentMaxBytes;
  private final java.util.Set<String> allowedAttachmentTypes;

  public SupportServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identityFacade,
      SupportTicketRepository tickets,
      SupportMessageRepository messages,
      com.routeshare.support.repository.SupportAttachmentRepository attachments,
      com.routeshare.storage.service.ObjectStoragePort storage,
      com.routeshare.storage.config.ObjectStorageProperties storageProperties,
      java.time.Clock clock,
      @org.springframework.beans.factory.annotation.Value(
              "${routeshare.support.attachment-max-bytes:10485760}")
          long attachmentMaxBytes,
      @org.springframework.beans.factory.annotation.Value(
              "${routeshare.support.attachment-allowed-types:image/jpeg,image/png,application/pdf}")
          String allowedAttachmentTypes) {
    this.current = current;
    this.identityFacade = identityFacade;
    this.tickets = tickets;
    this.messages = messages;
    this.attachments = attachments;
    this.storage = storage;
    this.storageProperties = storageProperties;
    this.clock = clock;
    this.attachmentMaxBytes = attachmentMaxBytes;
    this.allowedAttachmentTypes =
        java.util.Arrays.stream(allowedAttachmentTypes.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
  }

  @Override
  @Transactional
  public SupportTicketResponse create(String ownerRole, CreateTicketRequest req) {
    long appUserId = currentAppUserId();
    var ticket =
        tickets.save(
            SupportTicketEntity.open(
                appUserId, ownerRole, req.subject(), req.category(), req.priority()));
    messages.save(SupportMessageEntity.of(ticket.getId(), appUserId, ownerRole, req.message()));
    return toResponse(ticket, true);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SupportTicketResponse> listMine() {
    return tickets.findByAppUserIdOrderByIdDesc(currentAppUserId()).stream()
        .map(t -> toResponse(t, false))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public SupportTicketResponse getMine(long ticketId) {
    return toResponse(requireOwned(ticketId), true);
  }

  @Override
  @Transactional
  public SupportMessageResponse addMessage(
      String senderRole, long ticketId, TicketMessageRequest req) {
    var ticket = requireOwned(ticketId);
    var saved =
        messages.save(
            SupportMessageEntity.of(ticket.getId(), currentAppUserId(), senderRole, req.body()));
    // A new customer message reopens a resolved ticket for follow-up.
    if (SupportTicketEntity.RESOLVED.equals(ticket.getStatus())) {
      ticket.setStatus(SupportTicketEntity.OPEN);
    }
    ticket.setUpdatedAt(Instant.now());
    return toMessageResponse(saved);
  }

  @Override
  @Transactional
  public com.routeshare.support.dto.SupportAttachmentUploadResponse createAttachmentUpload(
      long ticketId, com.routeshare.support.dto.SupportAttachmentUploadRequest request) {
    requireOwned(ticketId);
    validateAttachmentMetadata(request.contentType(), request.sizeBytes());
    long appUserId = currentAppUserId();
    String extension =
        switch (request.contentType()) {
          case "image/jpeg" -> ".jpg";
          case "image/png" -> ".png";
          case "application/pdf" -> ".pdf";
          default -> "";
        };
    String key = "support/" + ticketId + "/" + java.util.UUID.randomUUID() + extension;
    var entity =
        attachments.save(
            SupportAttachmentEntity.reserve(
                ticketId,
                key,
                request.filename(),
                request.contentType(),
                request.sizeBytes(),
                appUserId));
    var ttl = java.time.Duration.ofSeconds(storageProperties.presignTtlSeconds());
    var upload = storage.createUploadUrl(key, request.contentType(), ttl);
    return new com.routeshare.support.dto.SupportAttachmentUploadResponse(
        entity.getId(),
        upload.url() == null ? null : upload.url().toString(),
        upload.httpMethod(),
        upload.headers(),
        clock.instant().plus(ttl));
  }

  @Override
  @Transactional
  public com.routeshare.support.dto.SupportAttachmentResponse submitAttachment(
      long ticketId, long attachmentId) {
    requireOwned(ticketId);
    var entity =
        attachments
            .findByIdAndTicketId(attachmentId, ticketId)
            .orElseThrow(() -> new java.util.NoSuchElementException("Attachment not found"));
    if (entity.getSubmittedAt() != null) {
      return toAttachmentResponse(entity);
    }
    if (!storage.exists(entity.getObjectKey())) {
      throw new IllegalStateException("Attachment upload has not completed");
    }
    String detected = detectContentType(storage.readPrefix(entity.getObjectKey(), 16));
    if (!entity.getContentType().equals(detected)) {
      storage.delete(entity.getObjectKey());
      throw new com.routeshare.common.errors.GateConflictException(
          "ATTACHMENT_TYPE_NOT_ALLOWED",
          "The uploaded file contents do not match an allowed attachment type.",
          "/support/tickets/" + ticketId);
    }
    entity.submit(clock.instant());
    return toAttachmentResponse(entity);
  }

  private void validateAttachmentMetadata(String contentType, long sizeBytes) {
    if (sizeBytes > attachmentMaxBytes) {
      throw new com.routeshare.common.errors.GateConflictException(
          "ATTACHMENT_TOO_LARGE",
          "Attachments may not exceed " + attachmentMaxBytes + " bytes.",
          "/support");
    }
    if (!allowedAttachmentTypes.contains(contentType)) {
      throw new com.routeshare.common.errors.GateConflictException(
          "ATTACHMENT_TYPE_NOT_ALLOWED",
          "Only JPEG, PNG and PDF attachments are allowed.",
          "/support");
    }
  }

  private String detectContentType(byte[] prefix) {
    if (prefix.length >= 4
        && prefix[0] == (byte) 0x89
        && prefix[1] == 0x50
        && prefix[2] == 0x4e
        && prefix[3] == 0x47) {
      return "image/png";
    }
    if (prefix.length >= 3
        && prefix[0] == (byte) 0xff
        && prefix[1] == (byte) 0xd8
        && prefix[2] == (byte) 0xff) {
      return "image/jpeg";
    }
    if (prefix.length >= 4
        && prefix[0] == 0x25
        && prefix[1] == 0x50
        && prefix[2] == 0x44
        && prefix[3] == 0x46) {
      return "application/pdf";
    }
    return "application/octet-stream";
  }

  private com.routeshare.support.dto.SupportAttachmentResponse toAttachmentResponse(
      SupportAttachmentEntity entity) {
    return new com.routeshare.support.dto.SupportAttachmentResponse(
        entity.getId(),
        entity.getFilename(),
        entity.getContentType(),
        entity.getSizeBytes(),
        entity.getSubmittedAt() == null ? "PENDING_UPLOAD" : "SUBMITTED",
        entity.getSubmittedAt());
  }

  private SupportTicketEntity requireOwned(long ticketId) {
    return tickets
        .findByIdAndAppUserId(ticketId, currentAppUserId())
        .orElseThrow(() -> new AccessDeniedException("Ticket does not belong to current user"));
  }

  private long currentAppUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  private SupportTicketResponse toResponse(SupportTicketEntity t, boolean withMessages) {
    List<SupportMessageResponse> msgs =
        withMessages
            ? messages.findBySupportTicketIdOrderByIdAsc(t.getId()).stream()
                .map(this::toMessageResponse)
                .toList()
            : List.of();
    return new SupportTicketResponse(
        t.getId(),
        t.getSubject(),
        t.getCategory(),
        t.getStatus(),
        t.getPriority(),
        t.getCreatedAt(),
        t.getUpdatedAt(),
        msgs);
  }

  private SupportMessageResponse toMessageResponse(SupportMessageEntity m) {
    return new SupportMessageResponse(m.getId(), m.getSenderRole(), m.getBody(), m.getCreatedAt());
  }
}
