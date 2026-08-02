package com.routeshare.trip.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.trip.domain.ArrivalDetector;
import com.routeshare.trip.entity.PickupWaitEntity;
import com.routeshare.trip.repository.PickupWaitRepository;
import com.routeshare.trip.service.ArrivalDetectionService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArrivalDetectionServiceImpl implements ArrivalDetectionService {
  private static final Logger log = LoggerFactory.getLogger(ArrivalDetectionServiceImpl.class);

  /**
   * How far back the trail is read. Wider than the dwell so a sample dropped by a flaky connection
   * does not reset a driver who has genuinely been parked at the corner.
   */
  private static final int TRAIL_WINDOW_MULTIPLIER = 4;

  private final PickupWaitRepository waits;
  private final PolicySettingService policy;
  private final NotificationFacade notifications;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final ArrivalDetector detector;
  private final Duration dwell;

  @Autowired
  public ArrivalDetectionServiceImpl(
      PickupWaitRepository waits,
      PolicySettingService policy,
      NotificationFacade notifications,
      ObjectMapper objectMapper,
      Clock clock,
      @Value("${routeshare.pickup-arrival.geofence-meters:120}") double geofenceMeters,
      @Value("${routeshare.pickup-arrival.dwell-seconds:30}") long dwellSeconds) {
    this.waits = waits;
    this.policy = policy;
    this.notifications = notifications;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.dwell = Duration.ofSeconds(dwellSeconds);
    this.detector = new ArrivalDetector(geofenceMeters, this.dwell);
  }

  @Override
  @Transactional
  public int onDriverLocation(long tripId) {
    List<Long> awaiting = waits.findBookingsAwaitingArrival(tripId);
    if (awaiting.isEmpty()) {
      return 0;
    }
    Instant since = clock.instant().minus(dwell.multipliedBy(TRAIL_WINDOW_MULTIPLIER));
    Duration wait = Duration.ofMinutes(policy.integer(PolicyKey.PICKUP_WAIT_MIN));

    int started = 0;
    for (Long bookingId : awaiting) {
      List<ArrivalDetector.Sample> trail =
          waits.findTrailAgainstPickup(tripId, bookingId, since).stream()
              .map(
                  row ->
                      new ArrivalDetector.Sample(
                          row.getSampleId(), row.getRecordedAt(), row.getDistanceMeters()))
              .toList();

      var arrival = detector.detect(trail);
      if (arrival.isEmpty()) {
        continue;
      }
      started += start(tripId, bookingId, arrival.get(), wait);
    }
    return started;
  }

  private int start(
      long tripId, long bookingId, ArrivalDetector.Arrival arrival, Duration waitDuration) {
    // The unique constraint on booking_id is the real guard: two samples arriving on different
    // threads both see no wait and both try to start one.
    if (waits.findByBookingId(bookingId).isPresent()) {
      return 0;
    }
    var wait =
        waits.save(
            PickupWaitEntity.startedOnArrival(
                tripId,
                bookingId,
                arrival.arrivedAt(),
                waitDuration,
                evidence(arrival.triggeringSampleIds())));

    waits
        .findPassengerAppUserId(bookingId)
        .ifPresent(
            passengerAppUserId ->
                notifications.notifyUser(
                    passengerAppUserId,
                    "DRIVER_ARRIVED",
                    "Your driver is here",
                    "Your driver has arrived at your pickup point and is waiting for you.",
                    Map.of(
                        "bookingId", String.valueOf(bookingId),
                        "expiresAt", wait.effectiveDeadline().toString())));

    log.info(
        "pickup wait started for booking {} on trip {}: arrived {}, expires {}",
        bookingId,
        tripId,
        arrival.arrivedAt(),
        wait.effectiveDeadline());
    return 1;
  }

  private String evidence(List<Long> sampleIds) {
    try {
      return objectMapper.writeValueAsString(Map.of("locationSampleIds", sampleIds));
    } catch (Exception e) {
      // Evidence is the point of the column, so a wait that cannot record why it started must not
      // start at all.
      throw new IllegalStateException("Unable to record the samples that triggered arrival", e);
    }
  }
}
