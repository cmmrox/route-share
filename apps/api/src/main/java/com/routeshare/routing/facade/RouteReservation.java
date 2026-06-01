package com.routeshare.routing.facade;

public record RouteReservation(
    long routePlanId, long routeOccurrenceId, double routeLengthMeters) {}
