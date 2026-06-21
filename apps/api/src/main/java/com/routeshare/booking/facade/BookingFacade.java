package com.routeshare.booking.facade;

import java.math.BigDecimal;
import java.util.Optional;

public interface BookingFacade {
  Optional<BigDecimal> findFareEstimateForPassengerBooking(long bookingId, long passengerAppUserId);

  Optional<BigDecimal> findDriverOwnedBookingFare(long bookingId, long driverAppUserId);

  /** Driver's app_user_id for a booking owned by the given passenger (also confirms ownership). */
  Optional<Long> findDriverAppUserIdForPassengerBooking(long bookingId, long passengerAppUserId);

  /** Passenger's app_user_id for a booking, used to address lifecycle notifications. */
  Optional<Long> findPassengerAppUserIdForBooking(long bookingId);
}
