package com.routeshare.admin.service.impl;

import com.routeshare.admin.dto.AdminSosResponse;
import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.admin.service.AdminSafetyService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.safety.entity.SosEventEntity;
import com.routeshare.safety.repository.SosEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSafetyServiceImpl implements AdminSafetyService {
  private static final int MAX_LIMIT = 200;

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final SosEventRepository sosEvents;
  private final NotificationFacade notifications;
  private final AdminAuditService audit;

  @Override
  @Transactional(readOnly = true)
  public List<AdminSosResponse> list(String status, int limit) {
    var page = PageRequest.of(0, Math.min(limit <= 0 ? 50 : limit, MAX_LIMIT));
    var rows =
        status == null || status.isBlank()
            ? sosEvents.findAllByOrderByIdDesc(page)
            : sosEvents.findByStatusOrderByIdDesc(status, page);
    return rows.stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public AdminSosResponse get(long sosEventId) {
    return toResponse(require(sosEventId));
  }

  @Override
  @Transactional
  public AdminSosResponse resolve(long sosEventId, String resolutionNote) {
    var event = require(sosEventId);
    long adminId = identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
    event.setStatus(SosEventEntity.RESOLVED);
    event.setResolvedAt(Instant.now());
    event.setResolvedBy(adminId);
    event.setResolutionNote(resolutionNote);
    audit.record("SOS_RESOLVED", "SOS_EVENT", String.valueOf(sosEventId), null);
    notifications.notifyUser(
        event.getAppUserId(),
        "SOS_RESOLVED",
        "Your SOS was resolved",
        "Our safety team has resolved your alert.",
        Map.of("sosEventId", String.valueOf(sosEventId)));
    return toResponse(event);
  }

  private SosEventEntity require(long sosEventId) {
    return sosEvents
        .findById(sosEventId)
        .orElseThrow(() -> new NoSuchElementException("SOS event not found"));
  }

  private AdminSosResponse toResponse(SosEventEntity e) {
    return new AdminSosResponse(
        e.getId(),
        e.getAppUserId(),
        e.getOwnerRole(),
        e.getStatus(),
        e.getTripId(),
        e.getBookingId(),
        e.getLatitude(),
        e.getLongitude(),
        e.getNote(),
        e.getCreatedAt(),
        e.getResolvedAt(),
        e.getResolvedBy(),
        e.getResolutionNote());
  }
}
