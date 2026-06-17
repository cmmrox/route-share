package com.routeshare.passenger.service.impl;

import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.passenger.dto.response.PassengerDocumentResponse;
import com.routeshare.passenger.entity.PassengerDocumentEntity;
import com.routeshare.passenger.repository.PassengerDocumentRepository;
import com.routeshare.passenger.service.PassengerDocumentService;
import com.routeshare.storage.config.ObjectStorageProperties;
import com.routeshare.storage.domain.DocumentUploadPolicy;
import com.routeshare.storage.dto.DownloadUrlResponse;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.dto.UploadUrlResponse;
import com.routeshare.storage.service.ObjectStoragePort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PassengerDocumentServiceImpl implements PassengerDocumentService {
  private static final String SCOPE = "passenger";

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final PassengerDocumentRepository documents;
  private final ObjectStoragePort storage;
  private final ObjectStorageProperties storageProps;
  private final DomainEventPublisher events;
  private final Clock clock;

  @Override
  @Transactional
  public UploadUrlResponse createUploadUrl(UploadUrlRequest req) {
    DocumentUploadPolicy.validate(req.contentType(), req.fileSizeBytes());
    long appUserId = currentAppUserId();
    String key =
        DocumentUploadPolicy.storageKey(SCOPE, appUserId, req.documentType(), req.contentType());
    var saved =
        documents.save(
            PassengerDocumentEntity.awaitingUpload(
                appUserId,
                req.documentType(),
                key,
                req.contentType(),
                req.fileSizeBytes(),
                req.originalFilename()));
    var ttl = Duration.ofSeconds(storageProps.presignTtlSeconds());
    var presigned = storage.createUploadUrl(key, req.contentType(), ttl);
    return new UploadUrlResponse(
        saved.getId(),
        key,
        presigned.url() == null ? null : presigned.url().toString(),
        presigned.httpMethod(),
        presigned.headers(),
        ttl.toSeconds());
  }

  @Override
  @Transactional
  public PassengerDocumentResponse submit(long documentId) {
    var doc = requireOwned(documentId);
    if (!storage.exists(doc.getStorageKey())) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT,
          "Uploaded file was not found in storage. Re-upload before submitting.");
    }
    doc.markSubmitted(Instant.now(clock));
    var response = toResponse(doc);
    events.publish(
        DomainEvent.of(
            "passenger.document.submitted",
            "passenger_document",
            String.valueOf(doc.getId()),
            "{\"documentType\":\"" + doc.getDocumentType() + "\"}"));
    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PassengerDocumentResponse> listMine() {
    return documents.findByAppUserIdOrderByIdDesc(currentAppUserId()).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public DownloadUrlResponse downloadUrl(long documentId) {
    var doc = requireOwned(documentId);
    var ttl = Duration.ofSeconds(storageProps.presignTtlSeconds());
    return new DownloadUrlResponse(
        storage.createDownloadUrl(doc.getStorageKey(), ttl).toString(), ttl.toSeconds());
  }

  private PassengerDocumentEntity requireOwned(long documentId) {
    return documents
        .findByIdAndAppUserId(documentId, currentAppUserId())
        .orElseThrow(() -> new AccessDeniedException("Document does not belong to current user"));
  }

  private long currentAppUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  private PassengerDocumentResponse toResponse(PassengerDocumentEntity e) {
    return new PassengerDocumentResponse(
        e.getId(),
        e.getDocumentType(),
        e.getStatus(),
        e.getContentType(),
        e.getFileSizeBytes(),
        e.getOriginalFilename(),
        e.getRejectionReason(),
        e.getSubmittedAt(),
        e.getReviewedAt(),
        e.getCreatedAt());
  }
}
