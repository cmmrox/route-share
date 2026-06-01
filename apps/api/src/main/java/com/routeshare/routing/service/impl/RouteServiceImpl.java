package com.routeshare.routing.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.routing.domain.RouteBucketCellGenerator;
import com.routeshare.routing.domain.RouteMatchCandidate;
import com.routeshare.routing.domain.RouteMatchScorer;
import com.routeshare.routing.domain.RouteSchedulePolicy;
import com.routeshare.routing.dto.request.CoordinateRequest;
import com.routeshare.routing.dto.request.RoutePublishRequest;
import com.routeshare.routing.dto.request.RouteSearchRequest;
import com.routeshare.routing.dto.response.RouteSearchResponse;
import com.routeshare.routing.repository.RouteBucketCellRepository;
import com.routeshare.routing.repository.RouteOccurrenceRepository;
import com.routeshare.routing.repository.RoutePlanRepository;
import com.routeshare.routing.repository.RouteScheduleRuleRepository;
import com.routeshare.routing.service.RouteService;
import com.routeshare.vehicle.facade.VehicleFacade;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RouteServiceImpl implements RouteService {
  public static final int MIN_ROUTE_POINTS = 2;
  public static final int MAX_ROUTE_POINTS = 500;
  private static final int DEFAULT_SEARCH_RADIUS_METERS = 1_000;
  private static final int DEFAULT_DEPARTURE_WINDOW_MINUTES = 120;
  private static final int DEFAULT_SEARCH_LIMIT = 20;
  private static final int ROUTE_BUCKET_RESOLUTION = 3;

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final DriverFacade driverFacade;
  private final VehicleFacade vehicleFacade;
  private final RoutePlanRepository routes;
  private final Clock clock;
  private final RouteMatchScorer routeMatchScorer;
  private final RouteSchedulePolicy routeSchedulePolicy;
  private final RouteBucketCellGenerator routeBucketCellGenerator;
  private final RouteScheduleRuleRepository scheduleRules;
  private final RouteOccurrenceRepository occurrences;
  private final RouteBucketCellRepository bucketCells;

  public RouteServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identityFacade,
      DriverFacade driverFacade,
      VehicleFacade vehicleFacade,
      RoutePlanRepository routes) {
    this(current, identityFacade, driverFacade, vehicleFacade, routes, Clock.systemUTC());
  }

  public RouteServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identityFacade,
      DriverFacade driverFacade,
      VehicleFacade vehicleFacade,
      RoutePlanRepository routes,
      Clock clock) {
    this(
        current,
        identityFacade,
        driverFacade,
        vehicleFacade,
        routes,
        clock,
        new RouteMatchScorer(),
        new RouteSchedulePolicy(clock),
        new RouteBucketCellGenerator(),
        null,
        null,
        null);
  }

  @Transactional
  public Map<String, Object> publish(RoutePublishRequest req) {
    validateCoordinates(req.coordinates());
    var generatedOccurrences = routeSchedulePolicy.generateOneTimeOccurrences(req.departureTime());
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    long driverId =
        driverFacade
            .findApprovedDriverProfileIdByAppUserId(app.appUserId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Approved driver profile is required before publishing routes"));
    requireApprovedVehicleWithCapacity(req.vehicleId(), driverId, req.availableSeats());
    long routePlanId =
        routes.create(
            driverId,
            req.vehicleId(),
            req.originLabel(),
            req.destinationLabel(),
            toLineStringPoints(req.coordinates()),
            req.departureTime(),
            req.availableSeats());
    long scheduleRuleId = scheduleRules.insertOneTimeRule(routePlanId, req.departureTime());
    long routeOccurrenceId =
        occurrences.insertOccurrence(
            routePlanId, generatedOccurrences.getFirst(), req.availableSeats());
    routeBucketCellGenerator
        .cellsFor(req.coordinates(), ROUTE_BUCKET_RESOLUTION)
        .forEach(
            cell ->
                bucketCells.insertCell(
                    routePlanId, routeOccurrenceId, ROUTE_BUCKET_RESOLUTION, cell));
    return Map.of(
        "routePlanId",
        routePlanId,
        "routeScheduleRuleId",
        scheduleRuleId,
        "routeOccurrenceId",
        routeOccurrenceId);
  }

  @Transactional(readOnly = true)
  public List<RouteSearchResponse> search(RouteSearchRequest req) {
    validateSearchRequest(req);
    identityFacade.upsertFromToken(current.requireCurrentUser());
    int pickupRadiusMeters = valueOrDefault(req.pickupRadiusMeters(), DEFAULT_SEARCH_RADIUS_METERS);
    int dropoffRadiusMeters =
        valueOrDefault(req.dropoffRadiusMeters(), DEFAULT_SEARCH_RADIUS_METERS);
    int departureWindowMinutes =
        valueOrDefault(req.departureWindowMinutes(), DEFAULT_DEPARTURE_WINDOW_MINUTES);
    int limit = valueOrDefault(req.limit(), DEFAULT_SEARCH_LIMIT);
    Instant windowStart =
        req.requestedDepartureTime().minus(Duration.ofMinutes(departureWindowMinutes));
    Instant windowEnd =
        req.requestedDepartureTime().plus(Duration.ofMinutes(departureWindowMinutes));

    return routes
        .findSearchCandidates(
            req.pickup().longitude(),
            req.pickup().latitude(),
            req.dropoff().longitude(),
            req.dropoff().latitude(),
            windowStart,
            windowEnd,
            req.seats(),
            pickupRadiusMeters,
            dropoffRadiusMeters,
            ROUTE_BUCKET_RESOLUTION,
            routeBucketCellGenerator.cellFor(req.pickup(), ROUTE_BUCKET_RESOLUTION),
            routeBucketCellGenerator.cellFor(req.dropoff(), ROUTE_BUCKET_RESOLUTION),
            limit)
        .stream()
        .map(this::toSearchResponse)
        .sorted(Comparator.comparing(RouteSearchResponse::score).reversed())
        .toList();
  }

  public void validateCoordinates(List<CoordinateRequest> coordinates) {
    if (coordinates == null || coordinates.size() < MIN_ROUTE_POINTS) {
      throw new IllegalArgumentException("Route must contain at least two coordinates");
    }
    if (coordinates.size() > MAX_ROUTE_POINTS) {
      throw new IllegalArgumentException("Route must not contain more than 500 coordinates");
    }
    CoordinateRequest previous = null;
    boolean hasMovement = false;
    for (CoordinateRequest coordinate : coordinates) {
      validateCoordinate(coordinate);
      if (previous != null && !samePoint(previous, coordinate)) {
        hasMovement = true;
      }
      previous = coordinate;
    }
    if (!hasMovement) {
      throw new IllegalArgumentException("Route must contain at least two distinct coordinates");
    }
  }

  private void validateSearchRequest(RouteSearchRequest req) {
    validateCoordinate(req.pickup());
    validateCoordinate(req.dropoff());
    if (samePoint(req.pickup(), req.dropoff())) {
      throw new IllegalArgumentException("Search pickup and drop-off must be different");
    }
    if (req.requestedDepartureTime() == null
        || req.requestedDepartureTime().isBefore(Instant.now(clock))) {
      throw new IllegalArgumentException("Search departure time must be now or in the future");
    }
    if (req.seats() < 1) {
      throw new IllegalArgumentException("At least one seat is required");
    }
  }

  private RouteSearchResponse toSearchResponse(RoutePlanRepository.RouteSearchCandidateRow row) {
    var candidate =
        new RouteMatchCandidate(
            row.getRoutePlanId(),
            row.getOriginLabel(),
            row.getDestinationLabel(),
            row.getAvailableSeats(),
            row.getRouteLengthMeters(),
            row.getPickupFraction(),
            row.getDropoffFraction(),
            row.getPickupDistanceMeters(),
            row.getDropoffDistanceMeters(),
            row.getOverlapDistanceMeters(),
            row.getRequestedDistanceMeters());
    var score = routeMatchScorer.score(candidate);
    return new RouteSearchResponse(
        row.getRoutePlanId(),
        row.getRouteOccurrenceId(),
        row.getOriginLabel(),
        row.getDestinationLabel(),
        row.getDepartureTime(),
        row.getAvailableSeats(),
        round(row.getRouteLengthMeters()),
        row.getPickupFraction(),
        row.getDropoffFraction(),
        round(row.getPickupDistanceMeters()),
        round(row.getDropoffDistanceMeters()),
        round(row.getOverlapDistanceMeters()),
        score.overlapPercent(),
        score.score(),
        score.explanation());
  }

  private void validateCoordinate(CoordinateRequest coordinate) {
    if (coordinate == null || coordinate.latitude() == null || coordinate.longitude() == null) {
      throw new IllegalArgumentException(
          "Invalid route coordinate. Latitude and longitude are required");
    }
    double latitude = coordinate.latitude();
    double longitude = coordinate.longitude();
    if (!Double.isFinite(latitude)
        || !Double.isFinite(longitude)
        || latitude < -90
        || latitude > 90
        || longitude < -180
        || longitude > 180) {
      throw new IllegalArgumentException(
          "Invalid route coordinate. Expected latitude/longitude in WGS84 bounds");
    }
  }

  private boolean samePoint(CoordinateRequest left, CoordinateRequest right) {
    return Double.compare(left.latitude(), right.latitude()) == 0
        && Double.compare(left.longitude(), right.longitude()) == 0;
  }

  private void requireApprovedVehicleWithCapacity(
      long vehicleId, long driverId, int requestedSeats) {
    if (!vehicleFacade.existsApprovedOwnedVehicleWithCapacity(
        vehicleId, driverId, requestedSeats)) {
      throw new IllegalStateException(
          "Approved vehicle does not belong to current driver or lacks capacity");
    }
  }

  private int valueOrDefault(Integer value, int defaultValue) {
    return value == null ? defaultValue : value;
  }

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  private String toLineStringPoints(List<CoordinateRequest> coordinates) {
    return coordinates.stream()
        .map(coordinate -> coordinate.longitude() + " " + coordinate.latitude())
        .reduce((left, right) -> left + "," + right)
        .orElseThrow();
  }
}
