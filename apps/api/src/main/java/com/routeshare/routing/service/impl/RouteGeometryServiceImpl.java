package com.routeshare.routing.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.maps.dto.CoordinateResponse;
import com.routeshare.routing.dto.response.RouteGeometryResponse;
import com.routeshare.routing.repository.RoutePlanRepository;
import com.routeshare.routing.service.RouteGeometryService;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serves the passenger-travelled segment of a published route from the stored PostGIS route line.
 * The geometry is the driver's actual route, so no billable maps-provider request is involved.
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RouteGeometryServiceImpl implements RouteGeometryService {
  public static final String SOURCE_ROUTE_PLAN = "route_plan";

  private final RoutePlanRepository routes;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional(readOnly = true)
  public RouteGeometryResponse occurrenceSegment(
      long routeOccurrenceId, double pickupFraction, double dropoffFraction) {
    validateFractions(pickupFraction, dropoffFraction);
    var row =
        routes
            .findOccurrenceSegment(routeOccurrenceId, pickupFraction, dropoffFraction)
            .orElseThrow(() -> new NoSuchElementException("Route occurrence not found"));
    List<CoordinateResponse> coordinates = parseLineString(row.getGeoJson());
    if (coordinates.size() < 2) {
      throw new NoSuchElementException("Route occurrence has no drawable segment");
    }
    long meters = row.getLengthMeters() == null ? 0 : Math.round(row.getLengthMeters());
    return new RouteGeometryResponse(coordinates, meters, SOURCE_ROUTE_PLAN);
  }

  private void validateFractions(double pickupFraction, double dropoffFraction) {
    if (!isFraction(pickupFraction) || !isFraction(dropoffFraction)) {
      throw new IllegalArgumentException("Route fractions must be between 0 and 1");
    }
    if (pickupFraction >= dropoffFraction) {
      throw new IllegalArgumentException(
          "Pickup fraction must be before drop-off fraction on the route");
    }
  }

  private boolean isFraction(double value) {
    return Double.isFinite(value) && value >= 0.0 && value <= 1.0;
  }

  /** Parses a GeoJSON LineString produced by PostGIS into map-ready coordinates. */
  private List<CoordinateResponse> parseLineString(String geoJson) {
    if (geoJson == null || geoJson.isBlank()) {
      return List.of();
    }
    try {
      JsonNode root = objectMapper.readTree(geoJson);
      if (!"LineString".equals(root.path("type").asText())) {
        return List.of();
      }
      List<CoordinateResponse> coordinates = new ArrayList<>();
      for (JsonNode point : root.path("coordinates")) {
        coordinates.add(new CoordinateResponse(point.path(1).asDouble(), point.path(0).asDouble()));
      }
      return coordinates;
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Stored route geometry could not be parsed", e);
    }
  }
}
