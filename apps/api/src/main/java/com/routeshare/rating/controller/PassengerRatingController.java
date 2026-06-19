package com.routeshare.rating.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.rating.dto.RateBookingRequest;
import com.routeshare.rating.dto.RatingResponse;
import com.routeshare.rating.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('PASSENGER')")
public class PassengerRatingController {
  private final RatingService service;

  public PassengerRatingController(RatingService service) {
    this.service = service;
  }

  @PostMapping("/api/v1/passenger/bookings/{bookingId}/rating")
  ApiResponse<RatingResponse> rate(
      @PathVariable long bookingId, @Valid @RequestBody RateBookingRequest req) {
    return ApiResponse.ok(service.ratePassengerBooking(bookingId, req));
  }
}
