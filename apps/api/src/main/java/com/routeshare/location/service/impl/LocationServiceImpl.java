package com.routeshare.location.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.location.cache.LatestLocationCache;
import com.routeshare.location.cache.LocationSnapshot;
import com.routeshare.location.domain.LocationConfidence;
import com.routeshare.location.dto.request.DriverLocationUpdateRequest;
import com.routeshare.location.dto.request.LocationUpdateRequest;
import com.routeshare.location.dto.response.AdminLiveTripResponse;
import com.routeshare.location.dto.response.LocationSnapshotResponse;
import com.routeshare.location.dto.response.LocationUpdateResponse;
import com.routeshare.location.dto.response.PassengerLiveTripStateResponse;
import com.routeshare.location.event.LocationRealtimePublisher;
import com.routeshare.location.repository.LocationPipelineRepository;
import com.routeshare.location.repository.LocationSampleRepository;
import com.routeshare.location.service.LocationService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationServiceImpl implements LocationService {
  private static final Duration MAX_DEVICE_CLOCK_SKEW = Duration.ofMinutes(10);
  private static final double MAX_ACCEPTABLE_ACCURACY_METERS = 100.0;
  private static final double MAX_REASONABLE_SPEED_MPS = 60.0;
  private static final double IMPOSSIBLE_JUMP_BUFFER_METERS = 100.0;

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final LocationSampleRepository locations;
  private final LatestLocationCache latestLocationCache;
  private final LocationRealtimePublisher realtimePublisher;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final com.routeshare.trip.facade.TripArrivalFacade arrivals;
  private final LocationPipelineRepository progress;

  @Autowired
  public LocationServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identityFacade,
      LocationSampleRepository locations,
      LatestLocationCache latestLocationCache,
      LocationRealtimePublisher realtimePublisher,
      ObjectMapper objectMapper,
      Clock clock,
      com.routeshare.trip.facade.TripArrivalFacade arrivals,
      LocationPipelineRepository progress) {
    this.current = current;
    this.identityFacade = identityFacade;
    this.locations = locations;
    this.latestLocationCache = latestLocationCache;
    this.realtimePublisher = realtimePublisher;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.arrivals = arrivals;
    this.progress = progress;
  }

  public LocationServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identityFacade,
      LocationSampleRepository locations,
      LatestLocationCache latestLocationCache,
      LocationRealtimePublisher realtimePublisher,
      Clock clock,
      com.routeshare.trip.facade.TripArrivalFacade arrivals) {
    this(
        current,
        identityFacade,
        locations,
        latestLocationCache,
        realtimePublisher,
        new ObjectMapper().findAndRegisterModules(),
        clock,
        arrivals,
        null);
  }

  @Override
  @Transactional
  public Map<String, Object> update(LocationUpdateRequest req) {
    validateDeviceTimestamp(req.deviceRecordedAt());
    validateAccuracy(req.accuracyMeters());
    validateSpeed(req.speedMps());
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    if (!locations.canUpdateTripLocation(req.tripId(), req.driverProfileId(), app.appUserId())) {
      throw new AccessDeniedException("Trip does not belong to current driver or is not active");
    }
    var driverRequest =
        new DriverLocationUpdateRequest(
            req.latitude(),
            req.longitude(),
            req.accuracyMeters(),
            req.speedMps(),
            req.bearingDegrees(),
            req.deviceRecordedAt(),
            null,
            null,
            null);
    var response = accept(req.tripId(), req.driverProfileId(), driverRequest);
    return Map.of("accepted", response.accepted(), "tripId", response.tripId());
  }

  @Override
  @Transactional
  public LocationUpdateResponse ingestDriverLocation(
      Long tripId, DriverLocationUpdateRequest request) {
    validateRequest(request);
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    Long driverProfileId =
        locations
            .findDriverProfileIdForActiveTrip(tripId, app.appUserId())
            .orElseThrow(
                () ->
                    new AccessDeniedException(
                        "Trip does not belong to current driver or is not active"));
    return accept(tripId, driverProfileId, request);
  }

  @Override
  @Transactional(readOnly = true)
  public PassengerLiveTripStateResponse getPassengerLiveTripState(Long tripId) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    if (!locations.passengerCanViewTrip(tripId, app.appUserId())) {
      throw new AccessDeniedException("Passenger is not allowed to view this trip");
    }
    var row = locations.findPassengerLiveTrip(tripId).orElseThrow();
    var latest = latestResponse(tripId);
    var progressRow = progress == null ? null : progress.progress(tripId).orElse(null);
    LocationConfidence confidence =
        progressRow == null
            ? latest == null || latest.stale()
                ? LocationConfidence.STALE
                : LocationConfidence.MATCHED
            : LocationConfidence.valueOf(progressRow.getConfidence());
    long age =
        progressRow == null || progressRow.getMatchedAt() == null
            ? latest == null
                ? 0
                : Math.max(
                    0, Duration.between(latest.deviceRecordedAt(), Instant.now(clock)).toSeconds())
            : Math.max(
                0, Duration.between(progressRow.getMatchedAt(), Instant.now(clock)).toSeconds());
    boolean available =
        latest != null
            && confidence != LocationConfidence.STALE
            && confidence != LocationConfidence.OFF_ROUTE;
    return new PassengerLiveTripStateResponse(
        row.getTripId(),
        row.getTripStatus(),
        row.getOriginLabel(),
        row.getDestinationLabel(),
        row.getDepartureTime(),
        latest,
        available,
        confidence,
        age);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminLiveTripResponse> getAdminLiveTrips(int limit) {
    int boundedLimit = Math.max(1, Math.min(limit, 100));
    return locations.findAdminLiveTrips(boundedLimit).stream()
        .map(
            row ->
                new AdminLiveTripResponse(
                    row.getTripId(),
                    row.getDriverProfileId(),
                    row.getDriverName(),
                    row.getTripStatus(),
                    row.getOriginLabel(),
                    row.getDestinationLabel(),
                    row.getDepartureTime(),
                    latestResponse(row.getTripId())))
        .toList();
  }

  private LocationUpdateResponse accept(
      Long tripId, Long driverProfileId, DriverLocationUpdateRequest request) {
    validateRequest(request);
    rejectImpossibleJump(tripId, request);
    Instant serverReceivedAt = Instant.now(clock);
    locations.insertSample(
        tripId,
        driverProfileId,
        request.longitude(),
        request.latitude(),
        request.accuracyMeters(),
        request.speedMps(),
        request.bearingDegrees(),
        request.deviceRecordedAt());
    var snapshot =
        new LocationSnapshot(
            tripId,
            driverProfileId,
            request.latitude(),
            request.longitude(),
            request.accuracyMeters(),
            request.speedMps(),
            request.bearingDegrees(),
            request.deviceRecordedAt(),
            serverReceivedAt);
    latestLocationCache.put(snapshot);
    var response =
        new LocationUpdateResponse(
            true,
            tripId,
            driverProfileId,
            serverReceivedAt,
            snapshot.toResponse(serverReceivedAt, latestLocationCache.ttl()));
    locations.insertLocationEvent(
        tripId, driverProfileId, "DRIVER_LOCATION_ACCEPTED", toJson(response));
    realtimePublisher.publishTripLocation(response);

    // The sample is committed before arrival is judged, so the detector reads the trail this
    // update is part of. Location reports movement; trip decides whether it amounts to an arrival.
    arrivals.onDriverLocation(tripId);
    return response;
  }

  private LocationSnapshotResponse latestResponse(Long tripId) {
    Instant now = Instant.now(clock);
    return latestLocationCache
        .findByTripId(tripId)
        .map(snapshot -> snapshot.toResponse(now, latestLocationCache.ttl()))
        .orElse(null);
  }

  private void validateRequest(DriverLocationUpdateRequest request) {
    validateDeviceTimestamp(request.deviceRecordedAt());
    validateAccuracy(request.accuracyMeters());
    validateSpeed(request.speedMps());
  }

  private void validateDeviceTimestamp(Instant deviceRecordedAt) {
    if (deviceRecordedAt == null
        || Duration.between(deviceRecordedAt, Instant.now(clock))
                .abs()
                .compareTo(MAX_DEVICE_CLOCK_SKEW)
            > 0) {
      throw new IllegalArgumentException("Location timestamp is stale or too far in future");
    }
  }

  private void validateAccuracy(Double accuracyMeters) {
    if (accuracyMeters != null && accuracyMeters > MAX_ACCEPTABLE_ACCURACY_METERS) {
      throw new IllegalArgumentException("Location accuracy is too low");
    }
  }

  private void validateSpeed(Double speedMps) {
    if (speedMps != null && speedMps > MAX_REASONABLE_SPEED_MPS) {
      throw new IllegalArgumentException("Location speed is not physically plausible");
    }
  }

  private void rejectImpossibleJump(Long tripId, DriverLocationUpdateRequest request) {
    latestLocationCache
        .findByTripId(tripId)
        .ifPresent(
            previous -> {
              double seconds =
                  Math.max(
                      1.0,
                      Math.abs(
                          Duration.between(previous.deviceRecordedAt(), request.deviceRecordedAt())
                                  .toMillis()
                              / 1000.0));
              double distanceMeters =
                  haversineMeters(
                      previous.latitude(),
                      previous.longitude(),
                      request.latitude(),
                      request.longitude());
              double allowedMeters =
                  (MAX_REASONABLE_SPEED_MPS * seconds) + IMPOSSIBLE_JUMP_BUFFER_METERS;
              if (distanceMeters > allowedMeters) {
                throw new IllegalArgumentException("Location update contains an impossible jump");
              }
            });
  }

  private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
    double earthRadiusMeters = 6_371_000.0;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadiusMeters * c;
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to serialize location event", ex);
    }
  }
}
