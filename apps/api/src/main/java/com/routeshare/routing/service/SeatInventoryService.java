package com.routeshare.routing.service;

import com.routeshare.routing.dto.response.SeatMapResponse;
import java.util.List;

/**
 * Named seats, per occurrence.
 *
 * <p>Inventory is a row per slot rather than a counter, which is what lets P08 offer the front seat
 * and what turns the last-seat race into a constraint instead of an arithmetic problem.
 */
public interface SeatInventoryService {

  /**
   * Creates the slots for an occurrence from its vehicle's capacity. Idempotent — publication,
   * occurrence generation and a repaired backfill can all call it.
   */
  int generateFor(long routeOccurrenceId);

  /** P08. */
  SeatMapResponse seatMap(long routeOccurrenceId);

  /**
   * Decides which slots a booking will hold.
   *
   * <p>Named slots are validated against the occurrence and returned in order; an unnamed request
   * takes the lowest free ones, which is what keeps a client that has never heard of seats working.
   * This does <em>not</em> hold them — the hold belongs to {@code booking}, in the same transaction
   * as the booking row, and the database arbitrates the race.
   *
   * @param requestedSeatIds slots the rider chose, or empty/null to let the server pick
   * @param seats how many are needed
   */
  List<Long> resolveSeatsForBooking(long routeOccurrenceId, List<Long> requestedSeatIds, int seats);

  /**
   * D13, read from the occurrence. Exposed here so {@code booking} never reaches into routing's
   * tables to find out how a trip sells its seats.
   */
  com.routeshare.routing.domain.ApprovalMode approvalModeFor(long routeOccurrenceId);
}
