package com.routeshare.penalty.facade;

import com.routeshare.penalty.dto.response.AppliedDuesResponse;
import java.math.BigDecimal;

/**
 * What the rest of the product needs from penalties, and nothing more.
 *
 * <p>Booking, trip and the timer sweeps all know when a promise was broken; none of them learns
 * what it costs. That separation is deliberate — evidence and policy change for different reasons,
 * and the module that decides a no-show happened should not also have to be redeployed when the fee
 * moves from 25% to 20%.
 */
public interface PenaltyFacade {

  /** Slice 05's pickup-wait expiry released her seat. */
  void assessPassengerNoShow(long bookingId, Long tripId);

  /** She cancelled once the car was already moving. */
  void assessPassengerCancelAfterStart(long bookingId, Long tripId);

  /** His grace ran out and she took the free cancel it unlocked. */
  void assessDriverLate(long bookingId);

  /** He cancelled a published trip inside the free window. */
  void assessDriverLateCancellation(long tripId);

  /** He never started, so the trip auto-cancelled. No fee; the record is the consequence. */
  void recordDriverMissedStart(long tripId);

  /** Attaches carried-over fees to a checkout (P09d), returning the lines it should show. */
  AppliedDuesResponse applyOutstandingDues(long appUserId, long bookingId);

  /** The carrying booking captured, so the fees it carried are paid. */
  void settleDuesForBooking(long bookingId);

  /** The carrying booking fell through; the fees ride on to the next one. */
  void releaseDuesForBooking(long bookingId);

  /**
   * Prices a cancellation before it happens, so P26 states a figure instead of a percentage.
   *
   * <p>The percentage is the caller's, because the rule that picks it belongs to the cancellation
   * terms; the arithmetic and the split belong here.
   */
  PricedPenalty priceCancellation(long bookingId, BigDecimal percent);

  /** A fee and its two halves, with the base it was taken from. */
  record PricedPenalty(
      BigDecimal fareBase,
      BigDecimal percent,
      BigDecimal feeAmount,
      BigDecimal victimShare,
      BigDecimal platformShare) {}
}
