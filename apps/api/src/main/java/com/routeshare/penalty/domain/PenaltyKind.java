package com.routeshare.penalty.domain;

import com.routeshare.platform.domain.PolicyKey;

/**
 * The five ways a promise gets broken, and who pays for each.
 *
 * <p>The rate is never inlined: each kind names the policy setting that prices it, so a commercial
 * change is a row in {@code platform.policy_setting} rather than a deployment.
 */
public enum PenaltyKind {
  /** She cancelled once the car was already moving. Cheaper than not turning up at all. */
  PASSENGER_CANCEL_AFTER_START(
      PolicyKey.PAX_CANCEL_AFTER_START_PCT, PenaltyRole.PASSENGER, PenaltyRole.DRIVER),

  /** She never boarded, and his wait ran out. The most expensive passenger-side outcome. */
  PASSENGER_NO_SHOW(PolicyKey.NO_SHOW_PENALTY_PCT, PenaltyRole.PASSENGER, PenaltyRole.DRIVER),

  /**
   * He was late past the grace, and she took the free cancel it unlocked. Priced on the driver's
   * net for that seat, not on what she paid — the fee is a share of his earnings, not of her fare.
   */
  DRIVER_LATE(PolicyKey.DRIVER_LATE_PENALTY_PCT, PenaltyRole.DRIVER, PenaltyRole.PASSENGER),

  /** He cancelled a published trip inside the free window, stranding everyone booked on it. */
  DRIVER_LATE_CANCELLATION(
      PolicyKey.LATE_CANCEL_PENALTY_PCT, PenaltyRole.DRIVER, PenaltyRole.PASSENGER),

  /**
   * He never started, so the trip auto-cancelled. D32b is explicit that this costs no fee — "no
   * earnings for you" is the whole consequence, alongside the reliability record. The assessment is
   * still written, at zero, so a driver asking why his month looks the way it does gets an answer.
   */
  DRIVER_MISSED_START(null, PenaltyRole.DRIVER, PenaltyRole.NONE);

  private final PolicyKey rateKey;
  private final PenaltyRole payerRole;
  private final PenaltyRole victimRole;

  PenaltyKind(PolicyKey rateKey, PenaltyRole payerRole, PenaltyRole victimRole) {
    this.rateKey = rateKey;
    this.payerRole = payerRole;
    this.victimRole = victimRole;
  }

  /** The policy setting that prices this kind, or null when it carries no fee. */
  public PolicyKey rateKey() {
    return rateKey;
  }

  public PenaltyRole payerRole() {
    return payerRole;
  }

  public PenaltyRole victimRole() {
    return victimRole;
  }

  public boolean carriesFee() {
    return rateKey != null;
  }

  /** A driver is never billed for a penalty; it comes out of the next completed trip's earnings. */
  public boolean isDriverPaid() {
    return payerRole == PenaltyRole.DRIVER;
  }
}
