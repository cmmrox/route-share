package com.routeshare.trip.service.impl;

import com.routeshare.common.errors.GateConflictException;
import com.routeshare.payment.facade.PaymentFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.service.ReliabilityService;
import com.routeshare.trip.domain.TripStatus;
import com.routeshare.trip.dto.response.StartWindowResponse;
import com.routeshare.trip.entity.TripStartWindowEntity;
import com.routeshare.trip.repository.TripRepository;
import com.routeshare.trip.repository.TripStartWindowRepository;
import com.routeshare.trip.service.TripStartWindowService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TripStartWindowServiceImpl implements TripStartWindowService {
  private static final Logger log = LoggerFactory.getLogger(TripStartWindowServiceImpl.class);

  private final TripStartWindowRepository windows;
  private final TripRepository trips;
  private final PolicySettingService policy;
  private final ReliabilityService reliability;
  private final PaymentFacade payments;
  private final MeterRegistry meters;
  private final Clock clock;

  @Autowired
  public TripStartWindowServiceImpl(
      TripStartWindowRepository windows,
      TripRepository trips,
      PolicySettingService policy,
      ReliabilityService reliability,
      PaymentFacade payments,
      MeterRegistry meters,
      Clock clock) {
    this.windows = windows;
    this.trips = trips;
    this.policy = policy;
    this.reliability = reliability;
    this.payments = payments;
    this.meters = meters;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void open(long tripId, Instant departsAt) {
    if (windows.findByTripId(tripId).isPresent()) {
      return;
    }
    windows.save(TripStartWindowEntity.opening(tripId, departsAt, bufferDuration()));
  }

  @Override
  @Transactional(readOnly = true)
  public StartWindowResponse window(long tripId) {
    return render(require(tripId));
  }

  @Override
  @Transactional
  public StartWindowResponse extend(long tripId) {
    TripStartWindowEntity window = require(tripId);
    if (window.isResolved()) {
      throw new GateConflictException(
          "START_WINDOW_EXPIRED",
          "This trip's start window has already closed.",
          "/driver/trips/" + tripId);
    }
    if (!window.extend(extendDuration(), extendLimit())) {
      // D32c should have disabled the button; reaching here means the driver raced their own UI.
      throw new GateConflictException(
          "EXTENSION_ALREADY_USED",
          "You have already used this trip's extension.",
          "/driver/trips/" + tripId);
    }
    long driverAppUserId = driverOf(tripId);
    reliability.record(
        driverAppUserId,
        ReliabilityRole.DRIVER,
        ReliabilityEventType.START_EXTENSION_USED,
        null,
        tripId,
        null);
    return render(windows.save(window));
  }

  @Override
  @Transactional
  public void resolveStarted(long tripId) {
    windows
        .findByTripId(tripId)
        .ifPresent(
            window -> {
              window.resolve(TripStartWindowEntity.RESOLUTION_STARTED, clock.instant());
              windows.save(window);
            });
  }

  @Override
  @Transactional
  public void resolveCancelled(long tripId) {
    windows
        .findByTripId(tripId)
        .ifPresent(
            window -> {
              window.resolve(TripStartWindowEntity.RESOLUTION_CANCELLED, clock.instant());
              windows.save(window);
            });
  }

  @Override
  @Transactional
  public int sweepExpired(int batchSize) {
    Instant now = clock.instant();
    List<TripStartWindowEntity> expired = windows.claimExpired(now, PageRequest.of(0, batchSize));
    int cancelled = 0;
    for (TripStartWindowEntity window : expired) {
      // The row was claimed under a pessimistic write lock, but re-check: a manual start may have
      // committed since the sweep read it, and auto-cancelling a moving trip is unforgivable.
      if (window.isResolved()) {
        continue;
      }
      autoCancel(window, now);
      cancelled++;
    }
    return cancelled;
  }

  /**
   * Auto-cancel charges nobody. Every hold is voided before the trip is marked cancelled, so a
   * failure part-way leaves holds released rather than a cancelled trip with live authorisations.
   */
  private void autoCancel(TripStartWindowEntity window, Instant now) {
    long tripId = window.getTripId();
    for (Long bookingId : windows.findLiveBookingIds(tripId)) {
      payments.voidForBooking(bookingId, "TRIP_AUTO_CANCELLED");
    }

    trips
        .findById(tripId)
        .ifPresent(
            trip -> {
              trip.setStatus(TripStatus.CANCELLED);
              trips.save(trip);
            });

    window.resolve(TripStartWindowEntity.RESOLUTION_AUTO_CANCELLED, now);
    windows.save(window);

    windows
        .findDriverAppUserId(tripId)
        .ifPresent(
            driverAppUserId ->
                reliability.record(
                    driverAppUserId,
                    ReliabilityRole.DRIVER,
                    ReliabilityEventType.MISSED_START,
                    null,
                    tripId,
                    null));

    meters.counter("routeshare_autocancels_total").increment();
    log.info(
        "trip {} auto-cancelled: start window closed at {} (departed {})",
        tripId,
        window.effectiveDeadline(),
        window.getDepartsAt());
  }

  private StartWindowResponse render(TripStartWindowEntity window) {
    Instant deadline = window.effectiveDeadline();
    long secondsRemaining = Math.max(0, Duration.between(clock.instant(), deadline).toSeconds());
    int limit = extendLimit();
    int missedStartLimit = policy.integer(PolicyKey.MISSED_START_LIMIT);

    int missedThisMonth = 0;
    var driver = windows.findDriverAppUserId(window.getTripId());
    if (driver.isPresent()) {
      missedThisMonth =
          reliability
              .counter(driver.get(), ReliabilityRole.DRIVER, reliability.currentPeriod())
              .getMissedStarts();
    }

    return new StartWindowResponse(
        window.getTripId(),
        window.getDepartsAt(),
        deadline,
        secondsRemaining,
        window.hasExtensionRemaining(limit) ? 1 : 0,
        (int) extendDuration().toMinutes(),
        secondsRemaining == 0 && !window.isResolved(),
        window.getResolution(),
        missedThisMonth,
        Math.max(0, missedStartLimit - missedThisMonth),
        "If this reaches zero the trip is cancelled, nobody is charged, and a missed start is"
            + " recorded against you.");
  }

  private TripStartWindowEntity require(long tripId) {
    return windows
        .findByTripId(tripId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "This trip has no start window"));
  }

  private long driverOf(long tripId) {
    return windows
        .findDriverAppUserId(tripId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Trip has no driver"));
  }

  private Duration bufferDuration() {
    return Duration.ofMinutes(policy.integer(PolicyKey.START_BUFFER_MIN));
  }

  private Duration extendDuration() {
    return Duration.ofMinutes(policy.integer(PolicyKey.START_EXTEND_MIN));
  }

  private int extendLimit() {
    return policy.integer(PolicyKey.START_EXTEND_LIMIT);
  }
}
