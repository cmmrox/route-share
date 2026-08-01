package com.routeshare.booking.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PassengerBookingDetailResponse(
    Long bookingId,
    Long routePlanId,
    Long routeOccurrenceId,
    Long tripId,
    String originLabel,
    String destinationLabel,
    Instant departureTime,
    Integer seats,
    String bookingStatus,
    String tripStatus,
    String passengerTripStatus,
    BigDecimal fareEstimate,
    String paymentStatus,
    /** What has actually happened to the money — see {@link Payment}. */
    Payment payment,
    Double pickupLatitude,
    Double pickupLongitude,
    Double dropoffLatitude,
    Double dropoffLongitude,
    BigDecimal pickupRouteFraction,
    BigDecimal dropoffRouteFraction,
    Instant createdAt) {

  /**
   * The block P11, P12, P22 and P24 each read differently: "authorised, not charged", "charged at
   * 6:15 PM", "never charged". A status string alone cannot answer the second of those, which is
   * why {@code capturedAt} is carried rather than derived.
   *
   * @param last4 the card's last four digits — the only card detail that ever leaves the server
   */
  public record Payment(
      String method,
      String status,
      Instant authorizedAt,
      Instant capturedAt,
      BigDecimal amount,
      String last4) {}
}
