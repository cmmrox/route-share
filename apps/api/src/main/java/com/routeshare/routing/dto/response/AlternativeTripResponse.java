package com.routeshare.routing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One of the trips P13, P22 and P24 offer instead of the one that fell through.
 *
 * <p>An empty list is a real answer and is shown as such. Offering nothing is kinder than offering
 * a trip that leaves in nine hours dressed as an alternative.
 */
public record AlternativeTripResponse(
    long routeOccurrenceId,
    String driverFirstName,
    String originLabel,
    String destinationLabel,
    Instant departsAt,
    int seatsAvailable,
    BigDecimal ratePerKm,
    BigDecimal matchPercent) {}
