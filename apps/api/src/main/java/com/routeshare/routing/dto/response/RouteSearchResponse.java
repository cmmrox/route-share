package com.routeshare.routing.dto.response;

import java.time.Instant;

public record RouteSearchResponse(
    long routePlanId,
    long routeOccurrenceId,
    String originLabel,
    String destinationLabel,
    Instant departureTime,
    int availableSeats,
    double routeLengthMeters,
    double pickupRouteFraction,
    double dropoffRouteFraction,
    double pickupDistanceMeters,
    double dropoffDistanceMeters,
    double overlapDistanceMeters,
    double overlapPercent,
    double score,
    String explanation) {}
