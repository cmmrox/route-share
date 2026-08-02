package com.routeshare.reliability.facade;

import com.routeshare.reliability.dto.response.EarlyDropAllowanceResponse;

/**
 * What other modules need from reliability, and nothing more.
 *
 * <p>Booking asks whether an early drop is adjusted; the app shell asks whether a passenger must
 * prepay. Neither learns what an event log is.
 */
public interface ReliabilityFacade {

  EarlyDropAllowanceResponse earlyDropAllowance(long appUserId);

  /**
   * @return true when this drop is repriced, false when the allowance is spent
   */
  boolean consumeEarlyDropAllowance(long appUserId, Long bookingId, Long tripId);

  /**
   * Whether this passenger has reached the no-show count that requires prepayment, for {@code
   * /me/context} so the app can warn her before she books rather than at the till.
   */
  boolean prepayRequired(long appUserId);
}
