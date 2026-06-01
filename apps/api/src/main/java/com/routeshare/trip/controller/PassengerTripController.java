package com.routeshare.trip.controller;

import com.routeshare.booking.dto.response.PassengerBookingDetailResponse;
import com.routeshare.booking.dto.response.PassengerBookingSummaryResponse;
import com.routeshare.booking.service.BookingService;
import com.routeshare.common.web.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passenger/trips")
@PreAuthorize("isAuthenticated()")
public class PassengerTripController {
  private final BookingService bookings;

  public PassengerTripController(BookingService bookings) {
    this.bookings = bookings;
  }

  @GetMapping("/current")
  ApiResponse<PassengerBookingDetailResponse> current() {
    return ApiResponse.ok(bookings.getCurrentPassengerTrip().orElse(null));
  }

  @GetMapping("/history")
  ApiResponse<List<PassengerBookingSummaryResponse>> history() {
    return ApiResponse.ok(bookings.listPassengerTripHistory());
  }
}
