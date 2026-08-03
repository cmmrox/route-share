package com.routeshare.booking.facade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface BookingFacade {
  Optional<BigDecimal> findFareEstimateForPassengerBooking(long bookingId, long passengerAppUserId);

  Optional<BigDecimal> findDriverOwnedBookingFare(long bookingId, long driverAppUserId);

  /** Driver's app_user_id for a booking owned by the given passenger (also confirms ownership). */
  Optional<Long> findDriverAppUserIdForPassengerBooking(long bookingId, long passengerAppUserId);

  /** Passenger's app_user_id for a booking, used to address lifecycle notifications. */
  Optional<Long> findPassengerAppUserIdForBooking(long bookingId);

  /** Participant and lifecycle facts used by the booking-scoped chat authorization boundary. */
  Optional<ChatContext> findChatContext(long bookingId);

  /**
   * The confirmed bookings a trip start must capture. One start charges every card on the trip, so
   * the payment side asks for them as a set rather than being told one at a time.
   */
  java.util.List<BookingToCharge> findConfirmedBookingsForTrip(long tripId);

  /** Records what happened to a booking's money, for the screens that state it precisely. */
  void recordPaymentState(
      long bookingId, String paymentMethod, String paymentStatus, java.time.Instant capturedAt);

  /**
   * Closes every open booking on an occurrence its driver has called off, releasing the seats they
   * held.
   *
   * <p>Status and seats only. Money is the caller's to settle — routing voids each authorisation
   * afterwards — because putting a gateway call inside this facade would close a dependency loop
   * between booking and payment for no gain.
   *
   * @return the bookings that were open, so each can be voided and its rider told
   */
  java.util.List<CancelledBooking> cancelOpenBookingsForOccurrence(
      long routeOccurrenceId, String reason, long actorAppUserId);

  /** The trip behind an occurrence, if one has been materialised. */
  Optional<Long> findTripIdForOccurrence(long routeOccurrenceId);

  record BookingToCharge(long bookingId, BigDecimal fare) {}

  record CancelledBooking(long bookingId, long passengerAppUserId, String previousStatus) {}

  record ChatContext(
      long bookingId,
      long passengerAppUserId,
      long driverAppUserId,
      String bookingStatus,
      Instant droppedOffAt) {}
}
