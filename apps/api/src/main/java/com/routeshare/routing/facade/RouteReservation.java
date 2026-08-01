package com.routeshare.routing.facade;

/**
 * @param vehicleId the vehicle whose assessed rate band prices this booking — carried here so
 *     booking never has to reach into routing's tables to find it
 */
public record RouteReservation(
    long routePlanId, long routeOccurrenceId, long vehicleId, double routeLengthMeters) {}
