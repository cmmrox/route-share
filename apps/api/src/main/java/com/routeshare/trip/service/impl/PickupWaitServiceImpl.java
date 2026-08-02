package com.routeshare.trip.service.impl;

import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.service.ReliabilityService;
import com.routeshare.routing.facade.RoutingFacade;
import com.routeshare.trip.domain.PassengerTripStatus;
import com.routeshare.trip.dto.response.PickupWaitResponse;
import com.routeshare.trip.entity.PickupWaitEntity;
import com.routeshare.trip.repository.PassengerTripStateRepository;
import com.routeshare.trip.repository.PickupWaitRepository;
import com.routeshare.trip.service.PickupWaitService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PickupWaitServiceImpl implements PickupWaitService {
  private static final Logger log = LoggerFactory.getLogger(PickupWaitServiceImpl.class);

  private final com.routeshare.common.security.CurrentUserProvider current;
  private final com.routeshare.identity.facade.IdentityFacade identityFacade;
  private final PickupWaitRepository waits;
  private final PassengerTripStateRepository passengerStates;
  private final RoutingFacade routing;
  private final PolicySettingService policy;
  private final ReliabilityService reliability;
  private final NotificationFacade notifications;
  private final com.routeshare.penalty.facade.PenaltyFacade penalties;
  private final DomainEventPublisher events;
  private final MeterRegistry meters;
  private final Clock clock;

  @Autowired
  public PickupWaitServiceImpl(
      com.routeshare.common.security.CurrentUserProvider current,
      com.routeshare.identity.facade.IdentityFacade identityFacade,
      PickupWaitRepository waits,
      PassengerTripStateRepository passengerStates,
      RoutingFacade routing,
      PolicySettingService policy,
      ReliabilityService reliability,
      NotificationFacade notifications,
      com.routeshare.penalty.facade.PenaltyFacade penalties,
      DomainEventPublisher events,
      MeterRegistry meters,
      Clock clock) {
    this.current = current;
    this.identityFacade = identityFacade;
    this.waits = waits;
    this.passengerStates = passengerStates;
    this.routing = routing;
    this.policy = policy;
    this.reliability = reliability;
    this.notifications = notifications;
    this.penalties = penalties;
    this.events = events;
    this.meters = meters;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public PickupWaitResponse driverWindow(long tripId, long bookingId) {
    requireDriverOwnsTrip(tripId, bookingId);
    return render(require(bookingId));
  }

  @Override
  @Transactional(readOnly = true)
  public PickupWaitResponse passengerWindow(long bookingId) {
    long appUserId = currentAppUserId();
    if (!waits.isBookingOwnedByPassengerAppUser(bookingId, appUserId)) {
      throw new AccessDeniedException("Booking does not belong to current user");
    }
    return render(require(bookingId));
  }

  @Override
  @Transactional
  public PickupWaitResponse extend(long tripId, long bookingId) {
    requireDriverOwnsTrip(tripId, bookingId);
    PickupWaitEntity wait = require(bookingId);
    if (wait.isResolved()) {
      throw new GateConflictException(
          "WAIT_NOT_STARTED",
          "This passenger's pickup wait has already ended.",
          "/driver/trips/" + tripId);
    }
    if (!wait.extend(extendDuration(), extendLimit())) {
      throw new GateConflictException(
          "EXTENSION_ALREADY_USED",
          "You have already used this passenger's extension.",
          "/driver/trips/" + tripId);
    }
    return render(waits.save(wait));
  }

  @Override
  @Transactional
  public PickupWaitResponse releaseSeat(long tripId, long bookingId) {
    requireDriverOwnsTrip(tripId, bookingId);
    PickupWaitEntity wait = require(bookingId);
    Instant now = clock.instant();
    if (wait.isResolved()) {
      return render(wait);
    }
    // The deadline is hers, and a driver who could release early could take a fee off somebody
    // standing on the pavement thirty seconds late. Asserted here rather than in the controller,
    // because the sweeper and the endpoint must obey the same rule.
    if (now.isBefore(wait.effectiveDeadline())) {
      throw new GateConflictException(
          "WAIT_NOT_EXPIRED",
          "This passenger's wait has not run out yet.",
          "/driver/trips/" + tripId + "/passengers/" + bookingId);
    }
    noShow(wait, now);
    return render(wait);
  }

  @Override
  @Transactional
  public void resolveBoarded(long tripId, long bookingId) {
    waits
        .findByBookingId(bookingId)
        .ifPresent(
            wait -> {
              wait.resolve(PickupWaitEntity.RESOLUTION_BOARDED, clock.instant());
              waits.save(wait);
            });
  }

  @Override
  @Transactional
  public int sweepExpired(int batchSize) {
    Instant now = clock.instant();
    List<PickupWaitEntity> expired = waits.claimExpired(now, PageRequest.of(0, batchSize));
    int released = 0;
    for (PickupWaitEntity wait : expired) {
      // She may have boarded between the sweep's read and this loop, and a no-show recorded
      // against a passenger already in the car is indefensible.
      if (wait.isResolved()) {
        continue;
      }
      noShow(wait, now);
      released++;
    }
    return released;
  }

  /**
   * The seat goes back to inventory, the passenger's trip state says NO_SHOW, the reliability log
   * records why, and slice 06 is told so it can price the penalty. This slice does not decide what
   * a no-show costs; it decides that one happened, and leaves a trail saying on what evidence.
   */
  private void noShow(PickupWaitEntity wait, Instant now) {
    long bookingId = wait.getBookingId();
    long tripId = wait.getTripId();

    waits
        .findBookingSeatHold(bookingId)
        .ifPresent(hold -> routing.releaseSeats(hold.getRouteOccurrenceId(), hold.getSeats()));

    passengerStates.ensureWaitingPickupStateForConfirmedBooking(tripId, bookingId);
    passengerStates.updateStatus(tripId, bookingId, PassengerTripStatus.NO_SHOW);

    wait.resolve(PickupWaitEntity.RESOLUTION_NO_SHOW, now);
    waits.save(wait);

    waits
        .findPassengerAppUserId(bookingId)
        .ifPresent(
            passengerAppUserId -> {
              reliability.record(
                  passengerAppUserId,
                  ReliabilityRole.PASSENGER,
                  ReliabilityEventType.NO_SHOW,
                  bookingId,
                  tripId,
                  null);
              notifications.notifyUser(
                  passengerAppUserId,
                  "BOOKING_NO_SHOW",
                  "Your seat was released",
                  "Your driver waited and has now left. Your seat has been released.",
                  Map.of("bookingId", String.valueOf(bookingId)));
            });

    // The fee, priced and split, in the same transaction that decided the no-show happened. Slice
    // 06 owns what it costs; this slice only owns the evidence that it did.
    penalties.assessPassengerNoShow(bookingId, tripId);

    events.publish(
        DomainEvent.of(
            "booking.noshow",
            "booking",
            String.valueOf(bookingId),
            """
            {"bookingId":%d,"tripId":%d,"arrivedAt":"%s","deadline":"%s","releasedAt":"%s"}"""
                .formatted(bookingId, tripId, wait.getArrivedAt(), wait.effectiveDeadline(), now)));

    meters.counter("routeshare_noshow_releases_total").increment();
    log.info(
        "seat released for booking {} on trip {}: arrived {}, waited until {}",
        bookingId,
        tripId,
        wait.getArrivedAt(),
        wait.effectiveDeadline());
  }

  private PickupWaitResponse render(PickupWaitEntity wait) {
    Instant now = clock.instant();
    Instant deadline = wait.effectiveDeadline();
    long secondsRemaining = Math.max(0, Duration.between(now, deadline).toSeconds());
    int prepayThreshold = policy.integer(PolicyKey.PAX_PREPAY_NO_SHOW_THRESHOLD);

    int noShowsThisMonth =
        waits
            .findPassengerAppUserId(wait.getBookingId())
            .map(
                passengerAppUserId ->
                    reliability
                        .counter(
                            passengerAppUserId,
                            ReliabilityRole.PASSENGER,
                            reliability.currentPeriod())
                        .getNoShows())
            .orElse(0);

    return new PickupWaitResponse(
        wait.getTripId(),
        wait.getBookingId(),
        wait.getArrivedAt(),
        deadline,
        secondsRemaining,
        wait.hasExtensionRemaining(extendLimit()) ? 1 : 0,
        (int) extendDuration().toMinutes(),
        secondsRemaining == 0 && !wait.isResolved(),
        !wait.isResolved() && !now.isBefore(deadline),
        wait.getResolution(),
        noShowsThisMonth,
        prepayThreshold,
        "If this reaches zero the seat is released, a no-show is recorded, and a fee applies.");
  }

  private long currentAppUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  /**
   * Holding the DRIVER role is not enough. Without the second check any driver on the platform
   * could read another driver's passenger's countdown, spend her extension, or release her seat.
   */
  private void requireDriverOwnsTrip(long tripId, long bookingId) {
    if (!waits.isTripOwnedByDriverAppUser(tripId, currentAppUserId())) {
      throw new AccessDeniedException("Trip does not belong to current driver");
    }
    if (!waits.existsForTripAndBooking(tripId, bookingId)) {
      throw new AccessDeniedException("Booking is not waiting on this trip");
    }
  }

  private PickupWaitEntity require(long bookingId) {
    return waits
        .findByBookingId(bookingId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    // 05-10: there is no way to start a wait by asking. It exists only once the
                    // location trail says the driver arrived and stayed.
                    "No pickup wait for this booking — the driver has not been detected at the"
                        + " pickup point"));
  }

  private Duration extendDuration() {
    return Duration.ofMinutes(policy.integer(PolicyKey.PICKUP_WAIT_EXTEND_MIN));
  }

  private int extendLimit() {
    return policy.integer(PolicyKey.PICKUP_WAIT_EXTEND_LIMIT);
  }
}
