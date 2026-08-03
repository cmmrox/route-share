package com.routeshare.booking.service;

import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.dto.request.BookingStatusTransitionRequest;
import com.routeshare.booking.dto.response.DriverBookingRequestResponse;
import com.routeshare.booking.dto.response.PassengerBookingDetailResponse;
import com.routeshare.booking.dto.response.PassengerBookingSummaryResponse;
import com.routeshare.routing.dto.response.AlternativeTripResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BookingService {
  Map<String, Object> book(BookingRequest req, String idempotencyKey);

  Map<String, Object> transition(long bookingId, BookingStatusTransitionRequest req);

  List<PassengerBookingSummaryResponse> listPassengerBookings();

  PassengerBookingDetailResponse getPassengerBooking(long bookingId);

  List<AlternativeTripResponse> alternatives(long bookingId);

  Optional<PassengerBookingDetailResponse> getCurrentPassengerTrip();

  List<PassengerBookingSummaryResponse> listPassengerTripHistory();

  List<DriverBookingRequestResponse> listDriverBookingRequests(Long tripId);

  Map<String, Object> approveByDriver(long bookingId);

  Map<String, Object> declineByDriver(long bookingId, String reason);
}
