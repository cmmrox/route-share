package com.routeshare.penalty.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * The dues this checkout carried over (P09d).
 *
 * <p>Each line names the trip and date it came from, because "LKR 49 — fees" on a checkout is the
 * kind of charge that becomes a support ticket.
 */
public record AppliedDuesResponse(List<Line> items, BigDecimal total) {

  public static AppliedDuesResponse empty() {
    return new AppliedDuesResponse(List.of(), BigDecimal.ZERO.setScale(2));
  }

  public record Line(long dueId, String what, String trip, Instant when, BigDecimal amount) {}
}
