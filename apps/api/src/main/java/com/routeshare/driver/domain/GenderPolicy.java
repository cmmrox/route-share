package com.routeshare.driver.domain;

/**
 * D35 — who a driver is willing to carry.
 *
 * <p>{@link #WOMEN_ONLY} is a safety feature rather than a filter, and it is gated twice: a driver
 * may only set it if her own NIC verifies her as female, and a rider may only book it if hers does.
 * Either gate on its own would be a hole — the first would let anyone advertise a women-only car,
 * the second would let anyone ride in one.
 */
public enum GenderPolicy {
  ANYONE,
  WOMEN_ONLY;

  public static GenderPolicy of(String value) {
    return value == null || value.isBlank() ? ANYONE : GenderPolicy.valueOf(value);
  }

  public boolean isWomenOnly() {
    return this == WOMEN_ONLY;
  }
}
