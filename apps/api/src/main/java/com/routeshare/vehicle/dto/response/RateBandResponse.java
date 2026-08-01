package com.routeshare.vehicle.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Everything D39 and D40 render, with no client-side arithmetic: the band, the rate inside it, the
 * four signed factors that justify it, and what that rate means for where the driver appears.
 */
public record RateBandResponse(
    long vehicleId,
    String vehicleLabel,
    String classKey,
    String classLabel,
    Range classBand,
    Range band,
    BigDecimal chosenRate,
    String status,
    String setBy,
    Instant setAt,
    List<Factor> factors,
    BigDecimal netEffect,
    Position position,
    List<Position> positions,
    ReviewRequest reviewRequest) {

  public record Range(BigDecimal min, BigDecimal max) {}

  /** A signed adjustment with the wording the driver reads. Typed by an admin, never computed. */
  public record Factor(String key, String label, String detail, BigDecimal delta) {}

  public record Position(String key, String label, String rank, String demand) {}

  public record ReviewRequest(
      Long requestId, String status, Instant requestedAt, int slaDays, String decisionNote) {}
}
