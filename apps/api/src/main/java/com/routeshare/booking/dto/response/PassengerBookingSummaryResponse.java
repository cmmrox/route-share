package com.routeshare.booking.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PassengerBookingSummaryResponse(
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
    Instant createdAt) {}
