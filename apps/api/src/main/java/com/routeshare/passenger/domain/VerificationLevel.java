package com.routeshare.passenger.domain;

/**
 * P31a–c. How far a rider has got with proving who she is.
 *
 * <p>None of these values is ever a reason to refuse an ordinary booking. Verification is a badge,
 * a ranking signal and the key to a verified-only trip — P31a's "Book, pay and ride as normal" is a
 * promise, and {@code VerificationNeverBlocksBookingTest} holds it.
 */
public enum VerificationLevel {
  NONE,
  PENDING,
  VERIFIED,
  REJECTED;

  public static VerificationLevel of(String value) {
    return value == null || value.isBlank() ? NONE : VerificationLevel.valueOf(value);
  }

  public boolean isVerified() {
    return this == VERIFIED;
  }
}
