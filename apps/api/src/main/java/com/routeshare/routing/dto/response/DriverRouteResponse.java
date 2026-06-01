package com.routeshare.routing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record DriverRouteResponse(
    Long routePlanId,
    Long routeOccurrenceId,
    Long vehicleId,
    String originLabel,
    String destinationLabel,
    Instant departureTime,
    Integer availableSeats,
    BigDecimal routeLengthMeters,
    String routeStatus,
    String occurrenceStatus) {}
