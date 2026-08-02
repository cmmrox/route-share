package com.routeshare.routing.domain;

import com.routeshare.common.errors.GateCodes;

/**
 * Whether a given rider may book a given trip, and — if not — which rule refused her.
 *
 * <p>The reason is carried as data rather than thrown, because the two callers need it differently:
 * booking states it, and search silently omits the trip. A rider learning from a trip's absence
 * that it was women-only is fine; being able to enumerate a driver's policy by asking is not.
 */
public record EligibilityDecision(boolean allowed, String reason, String message) {

  public static EligibilityDecision allow() {
    return new EligibilityDecision(true, null, null);
  }

  public static EligibilityDecision denyWomenOnly() {
    return new EligibilityDecision(
        false,
        GateCodes.NOT_ELIGIBLE_WOMEN_ONLY,
        "This driver carries women only. It's offered once your NIC verification confirms you as"
            + " female.");
  }

  public static EligibilityDecision denyVerifiedOnly() {
    return new EligibilityDecision(
        false,
        GateCodes.NOT_ELIGIBLE_VERIFIED_ONLY,
        "This driver accepts verified riders only. Verifying takes about two minutes.");
  }
}
