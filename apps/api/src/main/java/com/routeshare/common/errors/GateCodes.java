package com.routeshare.common.errors;

/**
 * The reasons an authenticated caller can be refused, as a stable contract-visible vocabulary.
 *
 * <p>A 403 with an empty body forces the app to guess which of six screens to show. Each code here
 * maps to exactly one prototype screen, and the same code appears pre-emptively on {@code
 * /api/v1/me/context} so the app can render the screen <em>before</em> the user taps something that
 * fails.
 */
public final class GateCodes {
  private GateCodes() {}

  /** S13 — account suspension. Outranks every driver gate and blocks both modes. */
  public static final String ACCOUNT_SUSPENDED = "ACCOUNT_SUSPENDED";

  /** S07 — never applied to drive. */
  public static final String DRIVER_PROFILE_MISSING = "DRIVER_PROFILE_MISSING";

  /** S08 — application under review. */
  public static final String DRIVER_REVIEW_PENDING = "DRIVER_REVIEW_PENDING";

  /** S09 — a KYC document was rejected. */
  public static final String DRIVER_APPLICATION_REJECTED = "DRIVER_APPLICATION_REJECTED";

  /** D34 — reliability deactivation; riding and payouts are unaffected. */
  public static final String DRIVER_DEACTIVATED = "DRIVER_DEACTIVATED";

  /** S12 — publish blocked: a required document has never been uploaded. */
  public static final String DOCUMENT_MISSING = "DOCUMENT_MISSING";

  /** S12 — publish blocked: a required document was rejected. */
  public static final String DOCUMENT_REJECTED = "DOCUMENT_REJECTED";

  /** S12 — publish blocked: a required document has passed its expiry. */
  public static final String DOCUMENT_EXPIRED = "DOCUMENT_EXPIRED";

  /** S12 — publish blocked: no approved vehicle. */
  public static final String VEHICLE_NOT_APPROVED = "VEHICLE_NOT_APPROVED";

  /** D40 — approved vehicle with no rate band. Wired in slice 02. */
  public static final String RATE_BAND_NOT_SET = "RATE_BAND_NOT_SET";

  /** D35 — women-only was set by a driver whose NIC does not verify her as female. */
  public static final String WOMEN_ONLY_NOT_AVAILABLE = "WOMEN_ONLY_NOT_AVAILABLE";

  /**
   * P07 — this trip carries women only, and the caller is not eligible.
   *
   * <p>Returned at booking, never in search: search simply omits the trip. A rider learning a trip
   * is women-only from its absence is fine; being able to enumerate a driver's policy by asking is
   * not.
   */
  public static final String NOT_ELIGIBLE_WOMEN_ONLY = "NOT_ELIGIBLE_WOMEN_ONLY";

  /** P07 — this trip carries verified riders only, and the caller is not verified. */
  public static final String NOT_ELIGIBLE_VERIFIED_ONLY = "NOT_ELIGIBLE_VERIFIED_ONLY";

  /** P29 — an identity capture arrived from anywhere but the in-app camera. */
  public static final String CAPTURE_SOURCE_NOT_ALLOWED = "CAPTURE_SOURCE_NOT_ALLOWED";

  /** P29 — the capture session lapsed before the four steps were finished. */
  public static final String VERIFICATION_SESSION_EXPIRED = "VERIFICATION_SESSION_EXPIRED";

  /** P31b — a second submission while a reviewer still has the first. */
  public static final String VERIFICATION_ALREADY_PENDING = "VERIFICATION_ALREADY_PENDING";
}
