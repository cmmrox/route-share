package com.routeshare.safety.service.impl;

import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.ratelimit.RateLimitProperties;
import com.routeshare.common.ratelimit.RateLimiter;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.safety.dto.RaiseSosRequest;
import com.routeshare.safety.dto.SosEventResponse;
import com.routeshare.safety.entity.SosEventEntity;
import com.routeshare.safety.repository.SosEventRepository;
import com.routeshare.safety.service.SosService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SosServiceImpl implements SosService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final SosEventRepository sosEvents;
  private final DomainEventPublisher events;
  private final NotificationFacade notifications;
  private final RateLimiter rateLimiter;
  private final RateLimitProperties rateLimits;

  @Override
  @Transactional
  public SosEventResponse raise(String ownerRole, RaiseSosRequest req) {
    long appUserId = currentAppUserId();
    rateLimiter.check(
        "sos",
        String.valueOf(appUserId),
        rateLimits.sosPerMinute(),
        java.time.Duration.ofMinutes(1));
    var saved =
        sosEvents.save(
            SosEventEntity.raise(
                appUserId,
                ownerRole,
                req.tripId(),
                req.bookingId(),
                req.latitude(),
                req.longitude(),
                req.note()));
    // Surface to the ops/safety pipeline (admin live feed consumes safety.sos.raised events).
    events.publish(
        DomainEvent.of(
            "safety.sos.raised",
            "sos_event",
            String.valueOf(saved.getId()),
            "{\"appUserId\":" + appUserId + ",\"role\":\"" + ownerRole + "\"}"));
    // Confirm receipt to the user who raised it.
    notifications.notifyUser(
        appUserId,
        "SOS_RECEIVED",
        "Help is on the way",
        "Your SOS alert was received. Our safety team has been notified.",
        Map.of("sosEventId", String.valueOf(saved.getId())));
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SosEventResponse> listMine() {
    return sosEvents.findByAppUserIdOrderByIdDesc(currentAppUserId()).stream()
        .map(this::toResponse)
        .toList();
  }

  private long currentAppUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  private SosEventResponse toResponse(SosEventEntity e) {
    return new SosEventResponse(
        e.getId(),
        e.getStatus(),
        e.getTripId(),
        e.getBookingId(),
        e.getLatitude(),
        e.getLongitude(),
        e.getNote(),
        e.getCreatedAt());
  }
}
