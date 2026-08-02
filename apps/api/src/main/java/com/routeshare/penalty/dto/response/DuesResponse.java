package com.routeshare.penalty.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * P25: what is owed, why, from which trip, and when.
 *
 * <p>{@code settled} carries P25b's empty state explicitly rather than leaving the client to infer
 * it from an empty list — "you have no outstanding fees" and "we could not load your fees" must not
 * look the same.
 */
public record DuesResponse(List<Item> items, BigDecimal total, boolean settled) {

  public record Item(
      long id,
      String what,
      String why,
      Instant when,
      String trip,
      BigDecimal amount,
      String method,
      String status,
      Long originBookingId,
      Long settledBookingId) {}
}
