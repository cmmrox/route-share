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
  public void recordPaymentState(
      long bookingId, String paymentMethod, String paymentStatus, java.time.Instant capturedAt) {
    bookings.updatePaymentState(bookingId, paymentMethod, paymentStatus, capturedAt);
  }
}
