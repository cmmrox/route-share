package com.routeshare.routing.domain;

/**
 * D09: a published trip is editable until its first seat sells, and frozen afterwards.
 *
 * <p><b>Computed, never stored.</b> A boolean column would have to be maintained on every booking,
 * cancellation, expiry and no-show release, and the first path that forgot would leave a trip
 * frozen with nobody on it — or worse, editable underneath somebody who has already paid. The rule
 * is two facts that are already true of the row, so it is derived from them.
 */
public final class TripEditability {
  public static final String PUBLISHED = "PUBLISHED";

  private TripEditability() {}

  /**
   * @param status the occurrence's own status
   * @param liveSeatHolds seats currently held by a booking that has not been released
   */
  public static boolean isEditable(String status, int liveSeatHolds) {
    return PUBLISHED.equalsIgnoreCase(status) && liveSeatHolds == 0;
  }

  /** The sentence D09 puts on the banner when a trip has been frozen. */
  public static String freezeReason(int liveSeatHolds) {
    return liveSeatHolds == 1
        ? "Someone has booked a seat, so the details are now fixed."
        : liveSeatHolds + " seats are booked, so the details are now fixed.";
  }
}
