package com.routeshare.location.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.location.dto.response.AdminLiveTripResponse;
import com.routeshare.location.service.LocationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/trips")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminLiveTripController {
  private final LocationService locations;

  @GetMapping("/live")
  ApiResponse<List<AdminLiveTripResponse>> liveTrips(@RequestParam(defaultValue = "50") int limit) {
    return ApiResponse.ok(locations.getAdminLiveTrips(limit));
  }
}
