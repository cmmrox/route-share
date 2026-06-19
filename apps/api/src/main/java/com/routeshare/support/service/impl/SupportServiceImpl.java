package com.routeshare.support.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.support.dto.CreateTicketRequest;
import com.routeshare.support.dto.SupportMessageResponse;
import com.routeshare.support.dto.SupportTicketResponse;
import com.routeshare.support.dto.TicketMessageRequest;
import com.routeshare.support.entity.SupportMessageEntity;
import com.routeshare.support.entity.SupportTicketEntity;
import com.routeshare.support.repository.SupportMessageRepository;
import com.routeshare.support.repository.SupportTicketRepository;
import com.routeshare.support.service.SupportService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupportServiceImpl implements SupportService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final SupportTicketRepository tickets;
  private final SupportMessageRepository messages;

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
