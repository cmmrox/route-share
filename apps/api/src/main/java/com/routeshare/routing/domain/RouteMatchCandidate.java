package com.routeshare.routing.domain;

public record RouteMatchCandidate(
    long routePlanId,
    String originLabel,
    String destinationLabel,
    int availableSeats,
    double routeLengthMeters,
    double pickupFraction,
    double dropoffFraction,
    double pickupDistanceMeters,
    double dropoffDistanceMeters,
    double overlapDistanceMeters,
    double requestedDistanceMeters) {}
