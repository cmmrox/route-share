package com.routeshare.location.service.impl;

import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.ratelimit.*;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.location.cache.*;
import com.routeshare.location.domain.*;
import com.routeshare.location.dto.request.*;
import com.routeshare.location.dto.response.*;
import com.routeshare.location.event.LocationRealtimePublisher;
import com.routeshare.location.repository.LocationPipelineRepository;
import com.routeshare.location.service.ApproachService;
import com.routeshare.location.service.LocationPipelineService;
import com.routeshare.trip.facade.TripArrivalFacade;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationPipelineServiceImpl implements LocationPipelineService {
  private static final Set<String> RUNNING =
      Set.of("STARTED", "ARRIVED_PICKUP", "PASSENGER_ONBOARD");

  private final CurrentUserProvider current;
  private final IdentityFacade identity;
  private final LocationPipelineRepository repository;
  private final LocationFilterChain filters;
  private final DeadReckoner deadReckoner;
  private final EtaCalculator etaCalculator;
  private final RouteProjector routeProjector;
  private final LocationPolicyResolver policies;
  private final LatestLocationCache latest;
  private final LocationRealtimePublisher realtime;
  private final TripArrivalFacade arrivals;
  private final ApproachService approaches;
  private final MeterRegistry meters;
  private final Clock clock;
  private final long offRouteGraceSeconds;
  private final RateLimiter rateLimiter;
  private final RateLimitProperties rateLimits;

  public LocationPipelineServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identity,
      LocationPipelineRepository repository,
      LocationFilterChain filters,
      DeadReckoner deadReckoner,
      EtaCalculator etaCalculator,
      RouteProjector routeProjector,
      LocationPolicyResolver policies,
      LatestLocationCache latest,
      LocationRealtimePublisher realtime,
      TripArrivalFacade arrivals,
      ApproachService approaches,
      MeterRegistry meters,
      Clock clock,
      RateLimiter rateLimiter,
      RateLimitProperties rateLimits,
      @Value("${routeshare.location.off-route-grace-seconds:60}") long offRouteGraceSeconds) {
    this.current = current;
    this.identity = identity;
    this.repository = repository;
    this.filters = filters;
    this.deadReckoner = deadReckoner;
    this.etaCalculator = etaCalculator;
    this.routeProjector = routeProjector;
    this.policies = policies;
    this.latest = latest;
    this.realtime = realtime;
    this.arrivals = arrivals;
    this.approaches = approaches;
    this.meters = meters;
    this.clock = clock;
    this.rateLimiter = rateLimiter;
    this.rateLimits = rateLimits;
    this.offRouteGraceSeconds = offRouteGraceSeconds;
  }

  @Override
  @Transactional
  public LocationBatchUpdateResponse ingest(long tripId, LocationBatchUpdateRequest request) {
    long appUserId = currentAppUserId();
    var access =
        repository
            .driverTripAccess(tripId, appUserId)
            .orElseThrow(() -> new AccessDeniedException("Trip does not belong to current driver"));
    if (!RUNNING.contains(access.getTripStatus())) {
      throw new GateConflictException(
          "TRIP_NOT_RUNNING",
          "Location can only be submitted for a running trip.",
          "/driver/trips");
    }
    rateLimiter.check(
        "location-ingest",
        String.valueOf(tripId),
        rateLimits.locationIngestPerMinute(),
        Duration.ofMinutes(1));

    List<LocationSampleRequest> ordered =
        request.samples().stream()
            .sorted(Comparator.comparing(LocationSampleRequest::capturedAt))
            .toList();
    List<LocationBatchUpdateResponse.RejectedSample> rejected = new ArrayList<>();
    int accepted = 0;
    Integer lastBattery = null;
    for (LocationSampleRequest input : ordered) {
      lastBattery = input.batteryPercent();
      if (repository.claimSample(tripId, input.sampleId()) == 0) {
        rejected.add(rejected(input.sampleId(), LocationRejectionReason.DUPLICATE));
        count(LocationRejectionReason.DUPLICATE);
        continue;
      }
      Instant now = Instant.now(clock);
      Instant capturedAt = clampFuture(input.capturedAt(), now);
      var previousRow = repository.progress(tripId).orElse(null);
      ProgressState previous = previousRow == null ? null : state(previousRow);
      List<RouteProjection> candidates =
          repository.projectCandidates(tripId, input.latitude(), input.longitude()).stream()
              .map(
                  row ->
                      new RouteProjection(
                          row.getFraction(), row.getOffsetMeters(), row.getRemainingMeters()))
              .toList();
      var projection =
          routeProjector.selectCandidate(
              candidates, previous == null ? null : previous.routeFraction());

      if (previous != null && capturedAt.isBefore(previous.matchedAt())) {
        store(
            tripId,
            access.getDriverProfileId(),
            input,
            capturedAt,
            false,
            LocationRejectionReason.OUT_OF_ORDER,
            projection);
        rejected.add(rejected(input.sampleId(), LocationRejectionReason.OUT_OF_ORDER));
        count(LocationRejectionReason.OUT_OF_ORDER);
        continue;
      }

      var observed =
          new ObservedLocation(
              input.sampleId(),
              capturedAt,
              input.latitude(),
              input.longitude(),
              input.accuracyMeters(),
              input.speedMps(),
              input.bearingDegrees(),
              input.batteryPercent());
      var result = filters.apply(observed, projection, previous, now);
      if (!result.accepted()) {
        var reason = result.rejection().orElseThrow();
        store(tripId, access.getDriverProfileId(), input, capturedAt, false, reason, projection);
        if (reason == LocationRejectionReason.BACKWARD_PROGRESS && previous != null) {
          repository.recordReversalCandidate(tripId, projection.fraction(), now);
        }
        if (reason == LocationRejectionReason.OFF_ROUTE) {
          repository.recordOffRoute(
              tripId,
              previous == null ? projection.fraction() : previous.routeFraction(),
              capturedAt,
              now,
              input.speedMps(),
              input.bearingDegrees(),
              input.latitude(),
              input.longitude(),
              offRouteGraceSeconds);
        }
        rejected.add(rejected(input.sampleId(), reason));
        count(reason);
        continue;
      }

      double smoothedSpeed =
          etaCalculator.smooth(previous == null ? null : previous.speedMps(), input.speedMps());
      store(tripId, access.getDriverProfileId(), input, capturedAt, true, null, projection);
      repository.upsertMatched(
          tripId,
          result.progressFraction(),
          capturedAt,
          now,
          smoothedSpeed,
          input.bearingDegrees(),
          input.latitude(),
          input.longitude(),
          result.reversalCandidateFraction(),
          result.reversalCandidateCount());
      publishAccepted(tripId, access.getDriverProfileId(), input, capturedAt, now);
      accepted++;
      meters.counter("routeshare_location_samples_total", "result", "accepted").increment();
    }
    if (accepted > 0) {
      // Arrival detection reads only accepted rows after V039.
      arrivals.onDriverLocation(tripId);
      approaches.evaluateForTrip(tripId);
    }
    TripProgressResponse currentProgress =
        repository.progress(tripId).isPresent() ? progress(tripId) : null;
    return new LocationBatchUpdateResponse(
        accepted, List.copyOf(rejected), currentProgress, policyFor(appUserId, lastBattery));
  }

  @Override
  @Transactional(readOnly = true)
  public TripProgressResponse progress(long tripId) {
    var row = repository.progress(tripId).orElseThrow();
    ProgressState state = state(row);
    Instant now = Instant.now(clock);
    var estimate = deadReckoner.estimate(state, row.getRouteLengthMeters().doubleValue(), now);
    double remaining =
        Math.max(0, row.getRouteLengthMeters().doubleValue() * (1 - estimate.routeFraction()));
    return new TripProgressResponse(
        tripId,
        estimate.routeFraction(),
        estimate.confidence(),
        state.matchedAt(),
        estimate.ageSeconds(),
        state.speedMps(),
        state.bearingDegrees(),
        estimate.confidence() == LocationConfidence.OFF_ROUTE,
        remaining,
        etaCalculator.etaSeconds(remaining, state.speedMps()));
  }

  @Override
  @Transactional(readOnly = true)
  public TripProgressResponse driverProgress(long tripId) {
    if (repository.driverTripAccess(tripId, currentAppUserId()).isEmpty()) {
      throw new AccessDeniedException("Trip does not belong to current driver");
    }
    return progress(tripId);
  }

  @Override
  @Transactional(readOnly = true)
  public LocationPolicyResponse policy(Integer batteryPercent) {
    return policyFor(currentAppUserId(), batteryPercent);
  }

  private LocationPolicyResponse policyFor(long appUserId, Integer batteryPercent) {
    var state = repository.policyState(appUserId);
    return policies.resolve(
        Boolean.TRUE.equals(state.getRunning()),
        Boolean.TRUE.equals(state.getPublished()),
        Boolean.TRUE.equals(state.getApproach()),
        batteryPercent);
  }

  private long currentAppUserId() {
    return identity.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  private Instant clampFuture(Instant capturedAt, Instant now) {
    if (capturedAt.isAfter(now.plusSeconds(30))) {
      return now;
    }
    return capturedAt;
  }

  private void store(
      long tripId,
      long driverProfileId,
      LocationSampleRequest input,
      Instant capturedAt,
      boolean accepted,
      LocationRejectionReason reason,
      RouteProjection projection) {
    repository.insertObservation(
        tripId,
        driverProfileId,
        input.sampleId(),
        input.latitude(),
        input.longitude(),
        input.accuracyMeters(),
        input.speedMps(),
        input.bearingDegrees(),
        input.batteryPercent(),
        capturedAt,
        accepted,
        reason == null ? null : reason.name(),
        projection.fraction(),
        projection.offsetMeters());
  }

  private void publishAccepted(
      long tripId,
      long driverProfileId,
      LocationSampleRequest input,
      Instant capturedAt,
      Instant receivedAt) {
    var snapshot =
        new LocationSnapshot(
            tripId,
            driverProfileId,
            input.latitude(),
            input.longitude(),
            input.accuracyMeters(),
            input.speedMps(),
            input.bearingDegrees(),
            capturedAt,
            receivedAt);
    latest.put(snapshot);
    var response =
        new LocationUpdateResponse(
            true,
            tripId,
            driverProfileId,
            receivedAt,
            snapshot.toResponse(receivedAt, latest.ttl()));
    realtime.publishTripLocation(response);
  }

  private ProgressState state(LocationPipelineRepository.ProgressRow row) {
    return new ProgressState(
        row.getRouteFraction().doubleValue(),
        LocationConfidence.valueOf(row.getConfidence()),
        row.getMatchedAt(),
        row.getUpdatedAt(),
        decimal(row.getSpeedMps()),
        decimal(row.getBearingDegrees()),
        row.getLatitude(),
        row.getLongitude(),
        row.getOffRouteSince(),
        decimal(row.getReversalCandidateFraction()),
        row.getReversalCandidateCount() == null ? 0 : row.getReversalCandidateCount());
  }

  private Double decimal(java.math.BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }

  private LocationBatchUpdateResponse.RejectedSample rejected(
      String sampleId, LocationRejectionReason reason) {
    return new LocationBatchUpdateResponse.RejectedSample(sampleId, reason);
  }

  private void count(LocationRejectionReason reason) {
    meters.counter("routeshare_location_rejections_total", "reason", reason.name()).increment();
    meters.counter("routeshare_location_samples_total", "result", "rejected").increment();
  }
}
