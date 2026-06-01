package com.routeshare.routing.domain;

public record RouteMatchScore(
    double score,
    double overlapPercent,
    double pickupProximityScore,
    double dropoffProximityScore,
    String explanation) {}
