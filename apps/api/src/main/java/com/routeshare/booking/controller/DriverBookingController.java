package com.routeshare.booking.controller;

import com.routeshare.booking.service.BookingService;
import com.routeshare.common.web.ApiResponse;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver/bookings")
@PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
public class DriverBookingController {
  private final BookingService bookings;

  public DriverBookingController(BookingService bookings) {
    this.bookings = bookings;
  }

  @PostMapping("/{bookingId}/approve")
  ApiResponse<Map<String, Object>> approve(@PathVariable long bookingId) {
    return ApiResponse.ok(bookings.approveByDriver(bookingId));
  }

  @PostMapping("/{bookingId}/decline")
  ApiResponse<Map<String, Object>> decline(
      @PathVariable long bookingId, @RequestBody(required = false) DeclineBookingRequest req) {
    return ApiResponse.ok(bookings.declineByDriver(bookingId, req == null ? null : req.reason()));
  }

  public record DeclineBookingRequest(String reason) {}
}
