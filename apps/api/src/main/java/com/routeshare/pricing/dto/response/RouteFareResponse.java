package com.routeshare.pricing.dto.response;

public record RouteFareResponse(
    long routeOccurrenceId, long onRouteDistanceMeters, FareQuoteResponse fare) {}
