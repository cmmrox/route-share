package com.routeshare.routing.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.routing.dto.response.RouteGeometryResponse;
import com.routeshare.routing.service.RouteGeometryService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stored route geometry for matched rides: the passenger map draws the driver's actual published
 * route segment straight from the database instead of a billable Google Directions call.
 */
@RestController
@RequestMapping("/api/v1/passenger/route-occurrences")
@PreAuthorize("isAuthenticated()")
@Validated
public class PassengerRouteGeometryController {
  private final RouteGeometryService geometry;

  public PassengerRouteGeometryController(RouteGeometryService geometry) {
    this.geometry = geometry;
  }

  @GetMapping("/{routeOccurrenceId}/geometry")
  ApiResponse<RouteGeometryResponse> occurrenceSegment(
      @PathVariable long routeOccurrenceId,
      @RequestParam @DecimalMin("0.0") @DecimalMax("1.0") double pickupFraction,
      @RequestParam @DecimalMin("0.0") @DecimalMax("1.0") double dropoffFraction) {
    return ApiResponse.ok(
        geometry.occurrenceSegment(routeOccurrenceId, pickupFraction, dropoffFraction));
  }
}
