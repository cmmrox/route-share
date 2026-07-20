package com.routeshare.maps.controller;

import com.routeshare.common.ratelimit.RateLimitProperties;
import com.routeshare.common.ratelimit.RateLimiter;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.maps.dto.PlaceSuggestionResponse;
import com.routeshare.maps.service.PlaceSearchService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Passenger-facing Google Places proxy. Every request here is billable, so calls are per-user rate
 * limited and carry an optional client autocomplete session token for Google session billing.
 */
@RestController
@RequestMapping("/api/v1/passenger/places")
@PreAuthorize("isAuthenticated()")
@Validated
public class PassengerPlaceSearchController {
  private final PlaceSearchService places;
  private final CurrentUserProvider current;
  private final RateLimiter rateLimiter;
  private final RateLimitProperties rateLimits;

  public PassengerPlaceSearchController(
      PlaceSearchService places,
      CurrentUserProvider current,
      RateLimiter rateLimiter,
      RateLimitProperties rateLimits) {
    this.places = places;
    this.current = current;
    this.rateLimiter = rateLimiter;
    this.rateLimits = rateLimits;
  }

  @GetMapping("/autocomplete")
  ApiResponse<List<PlaceSuggestionResponse>> autocomplete(
      @RequestParam @NotBlank @Size(min = 2, max = 120) String query,
      @RequestParam(required = false) Double latitude,
      @RequestParam(required = false) Double longitude,
      @RequestParam(required = false) @Size(max = 64) String sessionToken) {
    rateLimiter.check(
        "places-autocomplete",
        current.requireCurrentUser().subject(),
        rateLimits.placesAutocompletePerMinute(),
        Duration.ofMinutes(1));
    return ApiResponse.ok(places.autocomplete(query, latitude, longitude, sessionToken));
  }

  @GetMapping("/{placeId}")
  ApiResponse<PlaceSuggestionResponse> details(
      @PathVariable @NotBlank String placeId,
      @RequestParam(required = false) @Size(max = 64) String sessionToken) {
    rateLimiter.check(
        "places-details",
        current.requireCurrentUser().subject(),
        rateLimits.placesDetailsPerMinute(),
        Duration.ofMinutes(1));
    return ApiResponse.ok(places.details(placeId, sessionToken));
  }
}
