package com.routeshare.location.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.location.dto.request.DriverLocationUpdateRequest;
import com.routeshare.location.dto.response.LocationUpdateResponse;
import com.routeshare.location.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver/trips/{tripId}/location-updates")
@PreAuthorize("hasRole('DRIVER')")
@RequiredArgsConstructor
public class DriverLocationController {
  private final LocationService locations;

  @PostMapping
  ApiResponse<LocationUpdateResponse> ingest(
      @PathVariable Long tripId, @Valid @RequestBody DriverLocationUpdateRequest request) {
    return ApiResponse.ok(locations.ingestDriverLocation(tripId, request));
  }
}
