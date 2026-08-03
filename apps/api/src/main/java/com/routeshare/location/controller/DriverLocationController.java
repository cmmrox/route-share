package com.routeshare.location.controller;

import com.routeshare.common.security.DriverAccess;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.location.dto.request.LocationBatchUpdateRequest;
import com.routeshare.location.dto.response.LocationBatchUpdateResponse;
import com.routeshare.location.service.LocationPipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver/trips/{tripId}/location-updates")
@DriverAccess
@RequiredArgsConstructor
public class DriverLocationController {
  private final LocationPipelineService locations;

  @PostMapping
  ApiResponse<LocationBatchUpdateResponse> ingest(
      @PathVariable Long tripId, @Valid @RequestBody LocationBatchUpdateRequest request) {
    return ApiResponse.ok(locations.ingest(tripId, request));
  }
}
