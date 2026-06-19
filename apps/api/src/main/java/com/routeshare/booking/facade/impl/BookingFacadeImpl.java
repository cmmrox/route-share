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
}
