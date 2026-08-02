package com.routeshare.reliability.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * D28's panel, and D34's evidence.
 *
 * <p>{@code missedStartDetail} is why the counter is a projection of an event log rather than a
 * number incremented in place: D34 has to show the three misses with their dates, and "you are one
 * from deactivation" is not an answer to "which three?".
 */
public record DriverReliabilityResponse(
    LocalDate month,
    Counted missedStarts,
    Counted lateCancellations,
    int startExtensionsUsed,
    BigDecimal onTimeStartPct,
    BigDecimal acceptancePct,
    DeactivationRisk deactivationRisk,
    List<Occurrence> missedStartDetail) {

  public record Counted(int count, int limit) {}

  public record DeactivationRisk(int remaining, boolean deactivated) {}

  public record Occurrence(java.time.Instant occurredAt, Long tripId, String metadata) {}
}
