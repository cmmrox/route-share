package com.routeshare.booking.controller;

import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.dto.request.BookingStatusTransitionRequest;
import com.routeshare.booking.service.BookingService;
import com.routeshare.common.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@PreAuthorize("isAuthenticated()")
public class BookingController {
  private final BookingService bookings;

  public BookingController(BookingService bookings) {
    this.bookings = bookings;
  }

  @PostMapping
  ApiResponse<Map<String, Object>> book(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody BookingRequest req) {
    return ApiResponse.ok(bookings.book(req, idempotencyKey));
  }

  @PatchMapping("/{bookingId}/status")
  ApiResponse<Map<String, Object>> transition(
      @PathVariable long bookingId, @Valid @RequestBody BookingStatusTransitionRequest req) {
    return ApiResponse.ok(bookings.transition(bookingId, req));
  }
}
