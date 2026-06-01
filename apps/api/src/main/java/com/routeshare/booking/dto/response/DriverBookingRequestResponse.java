package com.routeshare.booking.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

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
    Instant createdAt) {}
