package com.routeshare.reliability.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** P39's panel. */
public record PassengerReliabilityResponse(
    LocalDate month,
    BigDecimal completionPct,
    NoShows noShows,
    int lateCancels,
    BigDecimal onTimeAtPickupPct,
    boolean prepayRequired) {

  public record NoShows(int count, int prepayThreshold) {}
}
