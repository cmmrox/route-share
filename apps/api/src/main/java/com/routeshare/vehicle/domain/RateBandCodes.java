package com.routeshare.vehicle.domain;

/**
 * Refusals specific to rate bands.
 *
 * <p>Separate from {@code GateCodes}, which is the screen-mapped vocabulary for "you cannot do this
 * yet". These are conflicts with an assessed band — the caller is allowed to act, the value they
 * sent is not allowed. The one overlap, {@code RATE_BAND_NOT_SET}, lives in {@code GateCodes}
 * because it really is a screen (D40).
 */
public final class RateBandCodes {
  private RateBandCodes() {}

  /** The driver picked a rate outside their assessed band. */
  public static final String RATE_OUTSIDE_BAND = "RATE_OUTSIDE_BAND";

  /** An admin typed a band outside the vehicle class's range. */
  public static final String BAND_OUTSIDE_CLASS = "BAND_OUTSIDE_CLASS";

  /** A re-assessment is already open for this vehicle (D39 allows one). */
  public static final String RATE_REVIEW_ALREADY_OPEN = "RATE_REVIEW_ALREADY_OPEN";

  /** More seats than the vehicle class carries. */
  public static final String SEATS_EXCEED_CLASS_CAP = "SEATS_EXCEED_CLASS_CAP";
}
