package com.routeshare.booking.controller;

import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.service.BookingService;
import com.routeshare.common.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  ApiResponse<Map<String, Object>> book(@Valid @RequestBody BookingRequest req) {
    return ApiResponse.ok(bookings.book(req));
  }
}
