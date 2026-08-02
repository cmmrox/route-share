package com.routeshare.booking.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * D14's request card.
 *
 * <p>Carries no gender and no NIC — neither is a fact a driver needs to decide on a request, and
 * both leave the server nowhere. The photo has already been resolved against P30 in the query, so a
 * {@code null} here means "not shown to you", never "none on file".
 */
public record DriverBookingRequestResponse(
    Long bookingId,
    Long routePlanId,
    Long routeOccurrenceId,
    Long tripId,
    Long passengerAppUserId,
    String passengerName,
    Integer seats,
    String status,
    BigDecimal fareEstimate,
    Double pickupLatitude,
    Double pickupLongitude,
    Double dropoffLatitude,
    Double dropoffLongitude,
    Instant createdAt,
    String passengerVerificationLevel,
    String passengerPhotoUrl) {}
