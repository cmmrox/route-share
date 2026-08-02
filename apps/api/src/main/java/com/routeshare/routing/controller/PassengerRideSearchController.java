package com.routeshare.routing.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.routing.dto.request.RouteSearchRequest;
import com.routeshare.routing.dto.response.RideSearchPageResponse;
import com.routeshare.routing.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passenger/ride-searches")
@PreAuthorize("isAuthenticated()")
public class PassengerRideSearchController {
  private final RouteService routes;
  private final com.routeshare.common.ratelimit.RateLimiter rateLimiter;
  private final com.routeshare.common.ratelimit.RateLimitProperties rateLimits;
  private final com.routeshare.common.security.CurrentUserProvider current;

  public PassengerRideSearchController(
      RouteService routes,
      com.routeshare.common.ratelimit.RateLimiter rateLimiter,
      com.routeshare.common.ratelimit.RateLimitProperties rateLimits,
      com.routeshare.common.security.CurrentUserProvider current) {
    this.routes = routes;
    this.rateLimiter = rateLimiter;
    this.rateLimits = rateLimits;
    this.current = current;
  }

  @PostMapping
  ApiResponse<RideSearchPageResponse> create(@Valid @RequestBody RouteSearchRequest req) {
    // The hottest query in the product, and a PostGIS scan over every published corridor. A human
    // refines a search a few times a minute; a loop does it a few hundred.
    rateLimiter.check(
        "ride-searches",
        current.requireCurrentUser().subject(),
        rateLimits.rideSearchPerMinute(),
        java.time.Duration.ofMinutes(1));
    return ApiResponse.ok(routes.search(req));
  }
}
