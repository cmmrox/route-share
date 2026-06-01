package com.routeshare.trip.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.trip.dto.request.TripTransitionRequest;
import com.routeshare.trip.service.TripService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
@PreAuthorize("isAuthenticated()")
public class TripController {
  private final TripService trips;

  public TripController(TripService trips) {
    this.trips = trips;
  }

  @PostMapping("/{id}/transition")
  ApiResponse<Map<String, Object>> transition(
      @PathVariable long id, @Valid @RequestBody TripTransitionRequest req) {
    return ApiResponse.ok(trips.transition(id, req));
  }
}
