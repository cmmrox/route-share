package com.routeshare.passenger.domain;

/**
 * Written by the verification decision, never self-declared and never user-editable.
 *
 * <p>It exists for exactly one purpose — deciding whether a rider may book a women-only trip — and
 * appears in no public profile, no search result and no booking payload. {@code
 * EligibilityContractTest} asserts that.
 */
public enum Gender {
  FEMALE,
  MALE,
  UNSPECIFIED;

  public static Gender of(String value) {
    return value == null || value.isBlank() ? UNSPECIFIED : Gender.valueOf(value);
  }
}
