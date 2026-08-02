package com.routeshare.reliability.service;

import com.routeshare.reliability.dto.response.EarlyDropAllowanceResponse;

/**
 * The early-drop allowance (P16, P16b, D22b): two fare-adjusted drops per calendar month.
 *
 * <p>The third and later drops are not refused. She is getting out of the car either way, and a
 * platform that answered "no" to somebody already standing at the roadside would be answering a
 * question nobody asked. What changes is the money: the seat is still released, and the fare she
 * agreed to stands.
 */
public interface EarlyDropAllowanceService {

  /** What P16 shows before she taps. */
  EarlyDropAllowanceResponse allowance(long appUserId);

  /**
   * Spends one adjusted drop if any remain.
   *
   * @return true when this drop is repriced, false when the allowance is spent and the fare stands
   */
  boolean consume(long appUserId, Long bookingId, Long tripId);
}
