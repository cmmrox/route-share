package com.routeshare.routing.domain;

/**
 * D13: who can book this trip.
 *
 * <p>The default is {@link #APPROVE_EACH} everywhere it is not stated. A driver who has not chosen
 * should be asked before a stranger is put in his car, and slice 08's driving preferences will
 * supply the account-level default that overrides this one.
 */
public enum ApprovalMode {
  /** Seats sell immediately; the card is authorised at once and the trip is confirmed. */
  INSTANT,

  /** Each request waits for the driver, and lapses if he never answers (D16). */
  APPROVE_EACH;

  public static ApprovalMode of(String value) {
    return value == null ? APPROVE_EACH : ApprovalMode.valueOf(value);
  }

  public boolean confirmsImmediately() {
    return this == INSTANT;
  }
}
