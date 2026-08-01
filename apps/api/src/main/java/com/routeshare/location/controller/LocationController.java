package com.routeshare.location.controller;

import com.routeshare.common.security.DriverAccess;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.location.dto.request.LocationUpdateRequest;
import com.routeshare.location.service.LocationService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/location")
@DriverAccess
public class LocationController {
  private final LocationService locations;

  public LocationController(LocationService locations) {
    this.locations = locations;
  }

  @PostMapping("/updates")
  ApiResponse<Map<String, Object>> update(@Valid @RequestBody LocationUpdateRequest req) {
    return ApiResponse.ok(locations.update(req));
  }
}
