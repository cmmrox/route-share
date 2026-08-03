package com.routeshare.location.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.location.dto.request.ApproachPositionRequest;
import com.routeshare.location.dto.response.ApproachResponse;
import com.routeshare.location.service.ApproachService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/passenger/bookings/{bookingId}")
@PreAuthorize("hasRole('PASSENGER')")
@RequiredArgsConstructor
public class PassengerApproachController {
  private final ApproachService approaches;

  @GetMapping("/approach")
  ApiResponse<ApproachResponse> approach(@PathVariable long bookingId) {
    return ApiResponse.ok(approaches.passengerApproach(bookingId));
  }

  @PostMapping("/approach-position")
  ApiResponse<ApproachResponse> position(
      @PathVariable long bookingId,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody ApproachPositionRequest request) {
    return ApiResponse.ok(approaches.updatePassengerPosition(bookingId, request));
  }
}
