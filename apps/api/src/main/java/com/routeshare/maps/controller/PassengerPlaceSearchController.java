package com.routeshare.maps.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.maps.dto.PlaceSuggestionResponse;
import com.routeshare.maps.service.PlaceSearchService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passenger/places")
@PreAuthorize("isAuthenticated()")
@Validated
public class PassengerPlaceSearchController {
  private final PlaceSearchService places;

  public PassengerPlaceSearchController(PlaceSearchService places) {
    this.places = places;
  }

  @GetMapping("/autocomplete")
  ApiResponse<List<PlaceSuggestionResponse>> autocomplete(
      @RequestParam @NotBlank @Size(min = 2, max = 120) String query,
      @RequestParam(required = false) Double latitude,
      @RequestParam(required = false) Double longitude) {
    return ApiResponse.ok(places.autocomplete(query, latitude, longitude));
  }

  @GetMapping("/{placeId}")
  ApiResponse<PlaceSuggestionResponse> details(@PathVariable @NotBlank String placeId) {
    return ApiResponse.ok(places.details(placeId));
  }
}
