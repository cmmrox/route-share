package com.routeshare.booking.facade.impl;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.booking.repository.BookingRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingFacadeImpl implements BookingFacade {
  private final BookingRepository bookings;
  private final com.routeshare.booking.repository.BookingStatusHistoryRepository statusHistory;
  private final com.routeshare.booking.repository.BookingSeatRepository bookingSeats;
  private final java.time.Clock clock;

  @Override
  public Optional<BigDecimal> findFareEstimateForPassengerBooking(
      long bookingId, long passengerAppUserId) {
    return bookings.findFareEstimateByBookingIdAndPassengerAppUserId(bookingId, passengerAppUserId);
  }

  @Override
  public Optional<BigDecimal> findDriverOwnedBookingFare(long bookingId, long driverAppUserId) {
    return bookings.findFareEstimateForDriverBooking(bookingId, driverAppUserId);
  }

  @Override
  public Optional<Long> findDriverAppUserIdForPassengerBooking(
      long bookingId, long passengerAppUserId) {
    return bookings.findDriverAppUserIdForPassengerBooking(bookingId, passengerAppUserId);
  }

  @Override
  public Optional<Long> findPassengerAppUserIdForBooking(long bookingId) {
    return bookings.findPassengerAppUserId(bookingId);
  }

  @Override
  public java.util.List<BookingToCharge> findConfirmedBookingsForTrip(long tripId) {
    return bookings.findConfirmedBookingsForTrip(tripId).stream()
        .map(row -> new BookingToCharge(row.getBookingId(), row.getFareEstimate()))
        .toList();
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public java.util.List<CancelledBooking> cancelOpenBookingsForOccurrence(
      long routeOccurrenceId, String reason, long actorAppUserId) {
    var open = bookings.findOpenBookingsForOccurrence(routeOccurrenceId);
    java.util.List<CancelledBooking> cancelled = new java.util.ArrayList<>(open.size());
    for (var row : open) {
      if (bookings.updateStatus(row.getBookingId(), "CANCELLED") != 1) {
        continue;
      }
      statusHistory.recordTransition(
          row.getBookingId(), row.getStatus(), "CANCELLED", actorAppUserId, reason);
      // The seats go back before anything else looks at inventory. A hold left behind a cancelled
      // booking removes a seat from the car for ever and nobody involved ever notices.
      releaseSeatHolds(row.getBookingId());
      cancelled.add(
          new CancelledBooking(row.getBookingId(), row.getPassengerAppUserId(), row.getStatus()));
    }
    return java.util.List.copyOf(cancelled);
  }

  @Override
  public Optional<Long> findTripIdForOccurrence(long routeOccurrenceId) {
    return bookings.findTripIdForOccurrence(routeOccurrenceId);
  }

  private void releaseSeatHolds(long bookingId) {
    var holds = bookingSeats.findLiveHolds(bookingId);
    holds.forEach(hold -> hold.release(clock.instant()));
    bookingSeats.saveAll(holds);
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public void recordPaymentState(
      long bookingId, String paymentMethod, String paymentStatus, java.time.Instant capturedAt) {
    bookings.updatePaymentState(bookingId, paymentMethod, paymentStatus, capturedAt);
  }
}
