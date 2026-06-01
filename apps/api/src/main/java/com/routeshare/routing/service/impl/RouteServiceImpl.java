package com.routeshare.routing.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.routing.dto.request.CoordinateRequest;
import com.routeshare.routing.dto.request.RoutePublishRequest;
import com.routeshare.routing.repository.RoutePlanRepository;
import com.routeshare.routing.service.RouteService;
import com.routeshare.vehicle.facade.VehicleFacade;
import java.time.Clock;
import java.time.Instant;
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

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final DriverFacade driverFacade;
  private final VehicleFacade vehicleFacade;
  private final RoutePlanRepository routes;
  private final Clock clock;

  public RouteServiceImpl(
      CurrentUserProvider current,
      IdentityFacade identityFacade,
      DriverFacade driverFacade,
      VehicleFacade vehicleFacade,
      RoutePlanRepository routes) {
    this(current, identityFacade, driverFacade, vehicleFacade, routes, Clock.systemUTC());
  }

  @Transactional
  public Map<String, Object> publish(RoutePublishRequest req) {
    validateCoordinates(req.coordinates());
    validateDepartureTime(req.departureTime());
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
    return Map.of("routePlanId", routePlanId);
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

  private void validateDepartureTime(Instant departureTime) {
    if (departureTime == null || !departureTime.isAfter(Instant.now(clock))) {
      throw new IllegalArgumentException("Route departure time must be in the future");
    }
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

  private String toLineStringPoints(List<CoordinateRequest> coordinates) {
    return coordinates.stream()
        .map(coordinate -> coordinate.longitude() + " " + coordinate.latitude())
        .reduce((left, right) -> left + "," + right)
        .orElseThrow();
  }
}
