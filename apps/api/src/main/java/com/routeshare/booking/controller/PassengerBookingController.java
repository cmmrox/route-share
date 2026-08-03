package com.routeshare.booking.controller;

import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.dto.request.BookingStatusTransitionRequest;
import com.routeshare.booking.dto.request.EarlyDropOffRequest;
import com.routeshare.booking.dto.response.EarlyDropOffResponse;
import com.routeshare.booking.dto.response.PassengerBookingDetailResponse;
import com.routeshare.booking.dto.response.PassengerBookingSummaryResponse;
import com.routeshare.booking.service.BookingService;
import com.routeshare.booking.service.EarlyDropOffService;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.payment.dto.response.ReceiptResponse;
import com.routeshare.payment.service.PaymentService;
import com.routeshare.routing.dto.response.AlternativeTripResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passenger/bookings")
@PreAuthorize("isAuthenticated()")
public class PassengerBookingController {
  private static final String CANCELLED = "CANCELLED";
  private final BookingService bookings;
  private final PaymentService payments;
  private final EarlyDropOffService earlyDropOff;
  private final com.routeshare.trip.facade.TripTimerFacade tripTimers;

  public PassengerBookingController(
      BookingService bookings,
      PaymentService payments,
      EarlyDropOffService earlyDropOff,
      com.routeshare.trip.facade.TripTimerFacade tripTimers) {
    this.bookings = bookings;
    this.payments = payments;
    this.earlyDropOff = earlyDropOff;
    this.tripTimers = tripTimers;
  }

  @PostMapping
  ApiResponse<Map<String, Object>> create(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody BookingRequest req) {
    return ApiResponse.ok(bookings.book(req, idempotencyKey));
  }

  @GetMapping
  ApiResponse<List<PassengerBookingSummaryResponse>> list() {
    return ApiResponse.ok(bookings.listPassengerBookings());
  }

  @GetMapping("/{bookingId}")
  ApiResponse<PassengerBookingDetailResponse> get(@PathVariable long bookingId) {
    return ApiResponse.ok(bookings.getPassengerBooking(bookingId));
  }

  @GetMapping("/{bookingId}/alternatives")
  ApiResponse<List<AlternativeTripResponse>> alternatives(@PathVariable long bookingId) {
    return ApiResponse.ok(bookings.alternatives(bookingId));
  }

  @GetMapping("/{bookingId}/receipt")
  ApiResponse<ReceiptResponse> receipt(@PathVariable long bookingId) {
    return ApiResponse.ok(payments.receipt(bookingId));
  }

  @PostMapping("/{bookingId}/cancel")
  ApiResponse<Map<String, Object>> cancel(
      @PathVariable long bookingId, @Valid @RequestBody CancelBookingRequest req) {
    return ApiResponse.ok(
        bookings.transition(
            bookingId, new BookingStatusTransitionRequest(CANCELLED, req.reason())));
  }

  /**
   * P38 / P38b: her side of the pickup wait — the same clock the driver sees, the fee it ends in,
   * and how many no-shows she has this month. She reads no policy figure to render it.
   */
  @GetMapping("/{bookingId}/pickup-window")
  ApiResponse<com.routeshare.trip.dto.response.PickupWaitResponse> pickupWindow(
      @PathVariable long bookingId) {
    return ApiResponse.ok(tripTimers.pickupWindowForBooking(bookingId));
  }

  /**
   * P26 and P34 read this same endpoint. Two screens asking the same question of two code paths is
   * how they come to disagree, and the disagreement surfaces as a fee the screen said was waived.
   */
  @GetMapping("/{bookingId}/cancellation-terms")
  ApiResponse<com.routeshare.trip.dto.response.CancellationTermsResponse> cancellationTerms(
      @PathVariable long bookingId) {
    return ApiResponse.ok(tripTimers.cancellationTerms(bookingId));
  }

  @PostMapping("/{bookingId}/early-drop-off")
  ApiResponse<EarlyDropOffResponse> earlyDropOff(
      @PathVariable long bookingId, @Valid @RequestBody EarlyDropOffRequest req) {
    return ApiResponse.ok(earlyDropOff.finalizeEarlyDropOff(bookingId, req));
  }

  public record CancelBookingRequest(String reason) {}
}
