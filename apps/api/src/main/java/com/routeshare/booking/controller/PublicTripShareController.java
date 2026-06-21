package com.routeshare.booking.controller;

import com.routeshare.booking.dto.response.PublicTripStatusResponse;
import com.routeshare.booking.service.TripShareService;
import com.routeshare.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Unauthenticated read access to a shared trip's live status via a time-boxed token. */
@RestController
@RequestMapping("/api/v1/public/trip-shares")
public class PublicTripShareController {
  private final TripShareService tripShares;

  public PublicTripShareController(TripShareService tripShares) {
    this.tripShares = tripShares;
  }

  @GetMapping("/{token}")
  ApiResponse<PublicTripStatusResponse> status(@PathVariable String token) {
    return ApiResponse.ok(tripShares.publicStatus(token));
  }
}
