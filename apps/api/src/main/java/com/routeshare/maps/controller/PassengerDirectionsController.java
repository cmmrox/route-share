package com.routeshare.maps.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.maps.dto.DirectionsResponse;
import com.routeshare.maps.service.DirectionsPort;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Road-following driving route for the passenger map (pickup → drop-off). */
@RestController
@RequestMapping("/api/v1/passenger/directions")
@PreAuthorize("isAuthenticated()")
@Validated
public class PassengerDirectionsController {
  private final DirectionsPort directions;

  public PassengerDirectionsController(DirectionsPort directions) {
    this.directions = directions;
  }

  @GetMapping
  ApiResponse<DirectionsResponse> route(
      @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double originLat,
      @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double originLng,
      @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double destLat,
      @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double destLng) {
    var result = directions.route(originLat, originLng, destLat, destLng);
    return ApiResponse.ok(
        new DirectionsResponse(
            result.coordinates(),
            result.distanceMeters(),
            result.durationSeconds(),
            result.source()));
  }
}
