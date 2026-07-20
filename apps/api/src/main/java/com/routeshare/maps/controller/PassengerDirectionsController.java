package com.routeshare.maps.controller;

import com.routeshare.common.ratelimit.RateLimitProperties;
import com.routeshare.common.ratelimit.RateLimiter;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.maps.dto.DirectionsResponse;
import com.routeshare.maps.service.DirectionsPort;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.time.Duration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Road-following driving route for the passenger map (pickup → drop-off). Rate limited per user
 * because uncached calls are billed by Google; matched rides should use the stored route geometry
 * endpoint instead.
 */
@RestController
@RequestMapping("/api/v1/passenger/directions")
@PreAuthorize("isAuthenticated()")
@Validated
public class PassengerDirectionsController {
  private final DirectionsPort directions;
  private final CurrentUserProvider current;
  private final RateLimiter rateLimiter;
  private final RateLimitProperties rateLimits;

  public PassengerDirectionsController(
      DirectionsPort directions,
      CurrentUserProvider current,
      RateLimiter rateLimiter,
      RateLimitProperties rateLimits) {
    this.directions = directions;
    this.current = current;
    this.rateLimiter = rateLimiter;
    this.rateLimits = rateLimits;
  }

  @GetMapping
  ApiResponse<DirectionsResponse> route(
      @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double originLat,
      @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double originLng,
      @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double destLat,
      @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double destLng) {
    rateLimiter.check(
        "directions",
        current.requireCurrentUser().subject(),
        rateLimits.directionsPerMinute(),
        Duration.ofMinutes(1));
    var result = directions.route(originLat, originLng, destLat, destLng);
    return ApiResponse.ok(
        new DirectionsResponse(
            result.coordinates(),
            result.distanceMeters(),
            result.durationSeconds(),
            result.source()));
  }
}
