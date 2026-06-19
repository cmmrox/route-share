package com.routeshare.admin.service.impl;

import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.admin.service.AdminSupportService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.support.dto.SupportMessageResponse;
import com.routeshare.support.dto.SupportTicketResponse;
import com.routeshare.support.dto.TicketMessageRequest;
import com.routeshare.support.entity.SupportMessageEntity;
import com.routeshare.support.entity.SupportTicketEntity;
import com.routeshare.support.repository.SupportMessageRepository;
import com.routeshare.support.repository.SupportTicketRepository;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSupportServiceImpl implements AdminSupportService {
  private static final int MAX_LIMIT = 200;

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final SupportTicketRepository tickets;
  private final SupportMessageRepository messages;
  private final AdminAuditService audit;

  @Override
  @Transactional(readOnly = true)
  public List<SupportTicketResponse> list(String status, int limit) {
    var page = PageRequest.of(0, Math.min(limit <= 0 ? 50 : limit, MAX_LIMIT));
    var rows =
        status == null || status.isBlank()
            ? tickets.findAllByOrderByIdDesc(page)
            : tickets.findByStatusOrderByIdDesc(status, page);
    return rows.stream().map(t -> toResponse(t, false)).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public SupportTicketResponse get(long ticketId) {
    return toResponse(require(ticketId), true);
  }

  @Override
  @Transactional
  public SupportTicketResponse updateStatus(long ticketId, String status) {
    var ticket = require(ticketId);
    ticket.setStatus(status);
    ticket.setUpdatedAt(Instant.now());
    audit.record(
        "SUPPORT_TICKET_STATUS",
        "SUPPORT_TICKET",
        String.valueOf(ticketId),
        "{\"status\":\"" + status + "\"}");
    return toResponse(ticket, true);
  }

  @Override
  @Transactional
  public SupportMessageResponse reply(long ticketId, TicketMessageRequest req) {
    var ticket = require(ticketId);
    long adminId = identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
    var saved =
        messages.save(SupportMessageEntity.of(ticket.getId(), adminId, "ADMIN", req.body()));
    // An admin reply moves an open ticket to pending-customer.
    if (SupportTicketEntity.OPEN.equals(ticket.getStatus())) {
      ticket.setStatus(SupportTicketEntity.PENDING);
    }
    ticket.setUpdatedAt(Instant.now());
    audit.record("SUPPORT_TICKET_REPLY", "SUPPORT_TICKET", String.valueOf(ticketId), null);
    return new SupportMessageResponse(
        saved.getId(), saved.getSenderRole(), saved.getBody(), saved.getCreatedAt());
  }

  private SupportTicketEntity require(long ticketId) {
    return tickets
        .findById(ticketId)
        .orElseThrow(() -> new NoSuchElementException("Ticket not found"));
  }

  private SupportTicketResponse toResponse(SupportTicketEntity t, boolean withMessages) {
    List<SupportMessageResponse> msgs =
        withMessages
            ? messages.findBySupportTicketIdOrderByIdAsc(t.getId()).stream()
                .map(
                    m ->
                        new SupportMessageResponse(
                            m.getId(), m.getSenderRole(), m.getBody(), m.getCreatedAt()))
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
}
