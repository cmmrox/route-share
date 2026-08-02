package com.routeshare.booking.service;

import com.routeshare.routing.domain.ApprovalMode;
import java.util.List;

/**
 * Claims and releases the named seats a booking holds.
 *
 * <p>Routing decides which slots exist and which are free; the hold itself lives here, in the same
 * transaction as the booking row, because a seat held without a booking behind it is inventory
 * nobody can sell and nobody can explain.
 */
public interface SeatHoldService {

  /**
   * Holds the slots for a new booking.
   *
   * @throws com.routeshare.common.errors.GateConflictException {@code SEATS_TAKEN} when another
   *     rider got there first — the unique index is what decides, not a prior read
   * @return the seats now held, as the booking response shows them
   */
  List<HeldSeat> hold(
      long bookingId, long routeOccurrenceId, List<Long> requestedSeatIds, int seats);

  /** Gives every live hold back. Idempotent, and safe to call on any terminal path. */
  int release(long bookingId);

  /** The seats a booking is holding, for its detail response. */
  List<HeldSeat> heldSeats(long bookingId);

  /** D13, read from the occurrence so the client never chooses its own approval mode. */
  ApprovalMode approvalModeFor(long routeOccurrenceId);

  record HeldSeat(long seatId, int slotIndex, String label, String subLabel) {}
}
