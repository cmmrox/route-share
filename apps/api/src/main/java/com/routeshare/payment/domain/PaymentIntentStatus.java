package com.routeshare.payment.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * What has actually happened to someone's money.
 *
 * <pre>
 *   PENDING → AUTHORIZED → CAPTURED → REFUNDED
 *      ↓          ↓
 *    FAILED     VOIDED
 * </pre>
 *
 * <p>{@code AUTHORIZED} is the state this product could not previously express. Without it, "we
 * have not asked your bank yet" and "your bank is holding this, and we have not taken it" were the
 * same row — and those are the two things every booking screen is at pains to distinguish.
 *
 * <p>{@code REQUIRES_CAPTURE} is the old name for {@code AUTHORIZED}; it is accepted on read so
 * pre-slice rows still transition, and never written.
 */
public enum PaymentIntentStatus {
  PENDING,
  AUTHORIZED,
  /**
   * @deprecated legacy alias for {@link #AUTHORIZED}; read-only.
   */
  @Deprecated
  REQUIRES_CAPTURE,
  CAPTURED,
  VOIDED,
  REFUNDED,
  FAILED;

  private static final Map<PaymentIntentStatus, Set<PaymentIntentStatus>> ALLOWED =
      Map.of(
          PENDING, EnumSet.of(AUTHORIZED, FAILED, VOIDED),
          AUTHORIZED, EnumSet.of(CAPTURED, VOIDED, FAILED),
          REQUIRES_CAPTURE, EnumSet.of(CAPTURED, VOIDED, FAILED),
          CAPTURED, EnumSet.of(REFUNDED),
          VOIDED, EnumSet.noneOf(PaymentIntentStatus.class),
          REFUNDED, EnumSet.noneOf(PaymentIntentStatus.class),
          FAILED, EnumSet.of(AUTHORIZED));

  public boolean canTransitionTo(PaymentIntentStatus target) {
    return ALLOWED.getOrDefault(this, Set.of()).contains(target);
  }

  /** True while the money is held but not taken — the state a void applies to. */
  public boolean isAuthorizedNotCaptured() {
    return this == AUTHORIZED || this == REQUIRES_CAPTURE;
  }

  public boolean isSettled() {
    return this == CAPTURED || this == REFUNDED;
  }

  public static PaymentIntentStatus of(String raw) {
    return raw == null ? PENDING : valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
  }
}
