package com.routeshare.trip.dto.response;

import java.time.Instant;

public record DriverTripResponse(
    Long tripId,
    Long routePlanId,
    Long routeOccurrenceId,
    String originLabel,
    String destinationLabel,
    Instant departureTime,
    String status,
    Long confirmedBookings,
    Integer bookedSeats,
    Instant startedAt,
    Instant completedAt) {}
