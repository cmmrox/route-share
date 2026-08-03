package com.routeshare.location.controller;

import com.routeshare.common.security.DriverAccess;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.location.dto.response.*;
import com.routeshare.location.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/driver")
@DriverAccess
@RequiredArgsConstructor
public class DriverRealtimeController {
  private final LocationPipelineService locations;
  private final ApproachService approaches;

  @GetMapping("/location-policy")
  ApiResponse<LocationPolicyResponse> policy(
      @RequestParam(required = false) Integer batteryPercent) {
    return ApiResponse.ok(locations.policy(batteryPercent));
  }

  @GetMapping("/trips/{tripId}/progress")
  ApiResponse<TripProgressResponse> progress(@PathVariable long tripId) {
    return ApiResponse.ok(locations.driverProgress(tripId));
  }

  @GetMapping("/trips/{tripId}/approach")
  ApiResponse<ApproachResponse> approach(@PathVariable long tripId) {
    return ApiResponse.ok(approaches.driverApproach(tripId));
  }
}
