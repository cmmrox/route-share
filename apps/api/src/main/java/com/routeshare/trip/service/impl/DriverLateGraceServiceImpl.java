package com.routeshare.trip.service.impl;

import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.trip.dto.response.CancellationTermsResponse;
import com.routeshare.trip.entity.DriverLateGraceEntity;
import com.routeshare.trip.repository.DriverLateGraceRepository;
import com.routeshare.trip.service.DriverLateGraceService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverLateGraceServiceImpl implements DriverLateGraceService {
  private static final Logger log = LoggerFactory.getLogger(DriverLateGraceServiceImpl.class);

  static final String REASON_DRIVER_LATE = "DRIVER_LATE";
  static final String REASON_OUTSIDE_PENALTY_WINDOW = "OUTSIDE_PENALTY_WINDOW";
  static final String REASON_LATE_CANCELLATION = "LATE_CANCELLATION";
  static final String REASON_ALREADY_CLOSED = "BOOKING_ALREADY_CLOSED";

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final DriverLateGraceRepository graces;
  private final PolicySettingService policy;
  private final NotificationFacade notifications;
  private final DomainEventPublisher events;
  private final Clock clock;
  private final double averageSpeedKmh;

  @Autowired
  public DriverLateGraceServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identityFacade,
      DriverLateGraceRepository graces,
      PolicySettingService policy,
      NotificationFacade notifications,
      DomainEventPublisher events,
      Clock clock,
      @Value("${routeshare.routing.average-speed-kmh:30}") double averageSpeedKmh) {
    this.current = current;
    this.identityFacade = identityFacade;
    this.graces = graces;
    this.policy = policy;
    this.notifications = notifications;
    this.events = events;
    this.clock = clock;
    this.averageSpeedKmh = averageSpeedKmh;
  }

  @Override
  @Transactional
  public void openForBooking(long bookingId) {
    if (graces.findByBookingId(bookingId).isPresent()) {
      return;
    }
    Optional<Instant> promised = graces.computePromisedPickupAt(bookingId, averageSpeedKmh);
    if (promised.isEmpty()) {
      // No occurrence or no geometry behind the booking: better no clock than one with an invented
      // deadline, since this deadline decides whether her cancel is free.
      log.warn("no promised pickup could be derived for booking {}; no grace opened", bookingId);
      return;
    }
    graces.stampPromisedPickupAt(bookingId, promised.get());
    graces.save(DriverLateGraceEntity.opening(bookingId, promised.get(), graceDuration()));
  }

  @Override
  @Transactional(readOnly = true)
  public CancellationTermsResponse cancellationTerms(long bookingId) {
    long appUserId = identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
    if (!graces.isBookingOwnedByPassengerAppUser(bookingId, appUserId)) {
      throw new AccessDeniedException("Booking does not belong to current user");
    }

    var context =
        graces
            .findCancellationContext(bookingId)
            .orElseThrow(() -> new java.util.NoSuchElementException("Booking not found"));
    Instant now = clock.instant();
    var grace = graces.findByBookingId(bookingId);
    BigDecimal penaltyPct = policy.decimal(PolicyKey.LATE_CANCEL_PENALTY_PCT);

    if (!"CONFIRMED".equalsIgnoreCase(context.getBookingStatus())) {
      return terms(
          bookingId,
          false,
          REASON_ALREADY_CLOSED,
          "This booking is no longer active.",
          BigDecimal.ZERO,
          false,
          grace,
          null);
    }

    // P34 first: a driver who has not turned up is the whole reason this endpoint exists, and it
    // outranks the timing window. Nothing is recorded against her for a cancel she did not cause.
    if (grace.isPresent() && grace.get().isUnlocked()) {
      return terms(
          bookingId,
          true,
          REASON_DRIVER_LATE,
          "Your driver is late, so cancelling is free and nothing is recorded against you.",
          BigDecimal.ZERO,
          false,
          grace,
          null);
    }

    // P26: the ordinary free window, far enough ahead of departure that the seat can be resold.
    Instant freeUntil = context.getDepartsAt().minus(freeWindow());
    if (now.isBefore(freeUntil)) {
      return terms(
          bookingId,
          true,
          REASON_OUTSIDE_PENALTY_WINDOW,
          "Cancelling now is free.",
          BigDecimal.ZERO,
          false,
          grace,
          null);
    }

    Long secondsUntilFree =
        grace
            .map(g -> Math.max(0, Duration.between(now, g.getGraceExpiresAt()).toSeconds()))
            .orElse(null);
    return terms(
        bookingId,
        false,
        REASON_LATE_CANCELLATION,
        "Cancelling now is close to departure, so a late-cancellation fee applies.",
        penaltyPct,
        true,
        grace,
        secondsUntilFree);
  }

  @Override
  @Transactional
  public void resolvePickedUp(long bookingId) {
    graces
        .findByBookingId(bookingId)
        .ifPresent(
            grace -> {
              grace.resolve(DriverLateGraceEntity.RESOLUTION_PICKED_UP, clock.instant());
              graces.save(grace);
            });
  }

  @Override
  @Transactional
  public void resolveCancelled(long bookingId) {
    graces
        .findByBookingId(bookingId)
        .ifPresent(
            grace -> {
              grace.resolve(
                  grace.isUnlocked()
                      ? DriverLateGraceEntity.RESOLUTION_FREE_CANCELLED
                      : DriverLateGraceEntity.RESOLUTION_EXPIRED,
                  clock.instant());
              graces.save(grace);
            });
  }

  @Override
  @Transactional
  public int sweepExpired(int batchSize) {
    Instant now = clock.instant();
    List<DriverLateGraceEntity> expired = graces.claimExpired(now, PageRequest.of(0, batchSize));
    int unlocked = 0;
    for (DriverLateGraceEntity grace : expired) {
      if (grace.isResolved() || grace.isUnlocked()) {
        continue;
      }
      // He may be standing at her door right now. The grace protects her from a driver who has not
      // arrived, and a detected arrival says he has, whatever the clock reads.
      if (graces.hasDriverArrived(grace.getBookingId())) {
        grace.resolve(DriverLateGraceEntity.RESOLUTION_PICKED_UP, now);
        graces.save(grace);
        continue;
      }
      unlockFreeCancel(grace, now);
      unlocked++;
    }
    return unlocked;
  }

  private void unlockFreeCancel(DriverLateGraceEntity grace, Instant now) {
    long bookingId = grace.getBookingId();
    grace.unlock(now);
    graces.save(grace);

    graces
        .findPassengerAppUserId(bookingId)
        .ifPresent(
            passengerAppUserId ->
                notifications.notifyUser(
                    passengerAppUserId,
                    "DRIVER_LATE",
                    "Your driver is running late",
                    "You can now cancel for free, and nothing will be recorded against you.",
                    Map.of("bookingId", String.valueOf(bookingId))));

    events.publish(
        DomainEvent.of(
            "booking.driver_late",
            "booking",
            String.valueOf(bookingId),
            """
            {"bookingId":%d,"promisedPickupAt":"%s","graceExpiredAt":"%s"}"""
                .formatted(bookingId, grace.getPromisedPickupAt(), grace.getGraceExpiresAt())));

    log.info(
        "free cancel unlocked for booking {}: promised {}, grace expired {}",
        bookingId,
        grace.getPromisedPickupAt(),
        grace.getGraceExpiresAt());
  }

  private CancellationTermsResponse terms(
      long bookingId,
      boolean free,
      String reasonCode,
      String explanation,
      BigDecimal penaltyPct,
      boolean recorded,
      Optional<DriverLateGraceEntity> grace,
      Long secondsUntilFreeCancel) {
    return new CancellationTermsResponse(
        bookingId,
        free,
        reasonCode,
        explanation,
        penaltyPct,
        recorded,
        grace.map(DriverLateGraceEntity::getPromisedPickupAt).orElse(null),
        grace.map(DriverLateGraceEntity::getUnlockedAt).orElse(null),
        grace.map(DriverLateGraceEntity::getGraceExpiresAt).orElse(null),
        secondsUntilFreeCancel);
  }

  private Duration graceDuration() {
    return Duration.ofMinutes(policy.integer(PolicyKey.DRIVER_LATE_GRACE_MIN));
  }

  private Duration freeWindow() {
    return Duration.ofHours(policy.integer(PolicyKey.DRIVER_CANCEL_FREE_HOURS));
  }
}
