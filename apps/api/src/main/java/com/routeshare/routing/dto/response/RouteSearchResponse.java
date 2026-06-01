package com.routeshare.routing.dto.response;

import java.time.Instant;

public record RouteSearchResponse(
    long routePlanId,
    String originLabel,
    String destinationLabel,
    Instant departureTime,
    int availableSeats,
    double routeLengthMeters,
    double pickupDistanceMeters,
    double dropoffDistanceMeters,
    double overlapDistanceMeters,
    double overlapPercent,
    double score,
    String explanation) {}
