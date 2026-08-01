package com.routeshare.routing.dto.response;

import com.routeshare.pricing.dto.response.FareQuoteResponse;
import java.math.BigDecimal;
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
    String explanation,
    long matchedDistanceMeters,
    /** Mirrors {@code fare.passengerPays}; kept so existing readers do not break. */
    BigDecimal estimatedFare,
    String currency,
    /** The full v2 quote, with the driver's cut withheld — this is a passenger surface. */
    FareQuoteResponse fare,
    String driverName,
    String vehicleMake,
    String vehicleModel,
    String vehicleRegistration,
    Integer vehicleSeatCount) {}
