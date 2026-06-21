package com.routeshare.booking.controller;

import com.routeshare.booking.dto.request.ShareTripRequest;
import com.routeshare.booking.dto.response.ShareTripResponse;
import com.routeshare.booking.service.TripShareService;
import com.routeshare.common.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passenger/bookings/{bookingId}")
@PreAuthorize("isAuthenticated()")
public class PassengerTripShareController {
  private final TripShareService tripShares;

  public PassengerTripShareController(TripShareService tripShares) {
    this.tripShares = tripShares;
  }

  @PostMapping("/share")
  ApiResponse<ShareTripResponse> share(
      @PathVariable long bookingId, @Valid @RequestBody(required = false) ShareTripRequest req) {
    return ApiResponse.ok(tripShares.share(bookingId, req));
  }

  @PostMapping("/share-link")
  ApiResponse<ShareTripResponse> shareLink(
      @PathVariable long bookingId, @Valid @RequestBody(required = false) ShareTripRequest req) {
    return ApiResponse.ok(tripShares.share(bookingId, req));
  }

  @DeleteMapping("/share/{token}")
  ApiResponse<Void> revoke(@PathVariable long bookingId, @PathVariable String token) {
    tripShares.revoke(bookingId, token);
    return ApiResponse.ok(null);
  }
}
