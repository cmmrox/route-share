package com.routeshare.location.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.location.dto.response.PassengerLiveTripStateResponse;
import com.routeshare.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passenger/trips")
@PreAuthorize("hasRole('PASSENGER')")
@RequiredArgsConstructor
public class PassengerLiveTripController {
  private final LocationService locations;

  @GetMapping("/{tripId}/live-state")
  ApiResponse<PassengerLiveTripStateResponse> liveState(@PathVariable Long tripId) {
    return ApiResponse.ok(locations.getPassengerLiveTripState(tripId));
  }
}
