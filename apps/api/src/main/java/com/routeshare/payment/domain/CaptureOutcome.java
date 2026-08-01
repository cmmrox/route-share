package com.routeshare.payment.domain;

import java.math.BigDecimal;

/**
 * What happened to one booking's money when a trip started.
 *
 * <p>One start captures N cards, and one declined bank must not stop the other passengers — or the
 * driver. Each booking therefore reports its own outcome rather than the request failing as a
 * whole.
 *
 * @param failureCode a safe code, never the gateway's message: passenger-facing responses must not
 *     carry provider detail
 */
public record CaptureOutcome(long bookingId, Result result, BigDecimal amount, String failureCode) {

  public enum Result {
    CAPTURED,
    /** Cash bookings have nothing to capture; the driver collects in the car. */
    SKIPPED_CASH,
    /** Already captured — a retried start, which must never charge twice. */
    ALREADY_CAPTURED,
    FAILED
  }

  public static CaptureOutcome captured(long bookingId, BigDecimal amount) {
    return new CaptureOutcome(bookingId, Result.CAPTURED, amount, null);
  }

  public static CaptureOutcome alreadyCaptured(long bookingId, BigDecimal amount) {
    return new CaptureOutcome(bookingId, Result.ALREADY_CAPTURED, amount, null);
  }

  public static CaptureOutcome skippedCash(long bookingId, BigDecimal amount) {
    return new CaptureOutcome(bookingId, Result.SKIPPED_CASH, amount, null);
  }

  public static CaptureOutcome failed(long bookingId, BigDecimal amount, String failureCode) {
    return new CaptureOutcome(bookingId, Result.FAILED, amount, failureCode);
  }
}
