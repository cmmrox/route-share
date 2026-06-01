package com.routeshare.trip.controller;

import com.routeshare.booking.dto.response.DriverBookingRequestResponse;
import com.routeshare.booking.service.BookingService;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.trip.domain.PassengerTripStatus;
import com.routeshare.trip.domain.TripStatus;
import com.routeshare.trip.dto.request.PassengerTripStateTransitionRequest;
import com.routeshare.trip.dto.request.PreTripChecklistRequest;
import com.routeshare.trip.dto.request.TripTransitionRequest;
import com.routeshare.trip.dto.response.DriverTripResponse;
import com.routeshare.trip.service.TripService;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver/trips")
@PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
public class DriverTripController {
  private final TripService trips;
  private final BookingService bookings;

  public DriverTripController(TripService trips, BookingService bookings) {
    this.trips = trips;
    this.bookings = bookings;
  }

  @GetMapping
  public ApiResponse<List<DriverTripResponse>> list() {
    return ApiResponse.ok(trips.listDriverTrips());
  }

  @GetMapping("/{tripId}")
  public ApiResponse<DriverTripResponse> get(@PathVariable long tripId) {
    return ApiResponse.ok(trips.getDriverTrip(tripId));
  }

  @GetMapping("/{tripId}/booking-requests")
  public ApiResponse<List<DriverBookingRequestResponse>> bookingRequests(
      @PathVariable long tripId) {
    return ApiResponse.ok(bookings.listDriverBookingRequests(tripId));
  }

  @PostMapping("/{tripId}/pre-trip-checklist")
  public ApiResponse<Map<String, Object>> preTripChecklist(
      @PathVariable long tripId, @RequestBody PreTripChecklistRequest req) {
    return ApiResponse.ok(trips.recordPreTripChecklist(tripId, req));
  }

  @PostMapping("/{tripId}/arrived-pickup")
  public ApiResponse<Map<String, Object>> arrivedPickup(@PathVariable long tripId) {
    return ApiResponse.ok(trips.markArrivedPickup(tripId));
  }

  @PostMapping("/{tripId}/start")
  public ApiResponse<Map<String, Object>> start(@PathVariable long tripId) {
    return transitionTrip(tripId, TripStatus.STARTED);
  }

  @PostMapping("/{tripId}/complete")
  public ApiResponse<Map<String, Object>> complete(@PathVariable long tripId) {
    return transitionTrip(tripId, TripStatus.COMPLETED);
  }

  @PostMapping("/{tripId}/passengers/{bookingId}/board")
  public ApiResponse<Map<String, Object>> board(
      @PathVariable long tripId, @PathVariable long bookingId) {
    return transitionPassenger(tripId, bookingId, PassengerTripStatus.BOARDED);
  }

  @PostMapping("/{tripId}/passengers/{bookingId}/no-show")
  public ApiResponse<Map<String, Object>> noShow(
      @PathVariable long tripId, @PathVariable long bookingId) {
    return transitionPassenger(tripId, bookingId, PassengerTripStatus.NO_SHOW);
  }

  @PostMapping("/{tripId}/passengers/{bookingId}/drop-off")
  public ApiResponse<Map<String, Object>> dropOff(
      @PathVariable long tripId, @PathVariable long bookingId) {
    return transitionPassenger(tripId, bookingId, PassengerTripStatus.DROPPED_OFF);
  }

  private ApiResponse<Map<String, Object>> transitionTrip(long tripId, TripStatus status) {
    return ApiResponse.ok(trips.transition(tripId, new TripTransitionRequest(status)));
  }

  private ApiResponse<Map<String, Object>> transitionPassenger(
      long tripId, long bookingId, PassengerTripStatus status) {
    return ApiResponse.ok(
        trips.transitionPassengerState(
            tripId,
            bookingId,
            new PassengerTripStateTransitionRequest(status, defaultPassengerReason(status))));
  }

  private String defaultPassengerReason(PassengerTripStatus status) {
    return switch (status) {
      case BOARDED -> "Driver marked boarded";
      case NO_SHOW -> "Driver marked no-show";
      case DROPPED_OFF -> "Driver marked dropped off";
      case WAITING_PICKUP -> "Driver reset passenger to waiting pickup";
    };
  }
}
