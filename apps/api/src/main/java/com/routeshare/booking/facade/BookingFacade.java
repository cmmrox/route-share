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

  /**
   * The confirmed bookings a trip start must capture. One start charges every card on the trip, so
   * the payment side asks for them as a set rather than being told one at a time.
   */
  java.util.List<BookingToCharge> findConfirmedBookingsForTrip(long tripId);

  /** Records what happened to a booking's money, for the screens that state it precisely. */
  void recordPaymentState(
      long bookingId, String paymentMethod, String paymentStatus, java.time.Instant capturedAt);

  record BookingToCharge(long bookingId, BigDecimal fare) {}
}
