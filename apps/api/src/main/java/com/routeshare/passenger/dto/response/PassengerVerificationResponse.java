package com.routeshare.passenger.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * P28 and P31a–c: where a rider stands, what it unlocked, and — if a reviewer said no — which step
 * and why.
 *
 * <p>{@code blocksBooking} is always false and is returned anyway. P31a's promise is "Book, pay and
 * ride as normal", and a client that has to infer that from the absence of a field will eventually
 * infer it wrongly.
 */
public record PassengerVerificationResponse(
    String level,
    Instant verifiedOn,
    boolean blocksBooking,
    Long sessionId,
    List<VerificationSessionResponse.Step> steps,
    List<Benefit> benefits,
    String rejectionReason) {

  public record Benefit(String title, String description) {}

  /** P28's three reasons, in the order a rider cares about. */
  public static List<Benefit> benefits(int extraAcceptancePercent) {
    return List.of(
        new Benefit(
            "You appear higher in a driver's request list",
            "Approve-each-request drivers see verified riders first, so fewer of your requests"
                + " time out — around "
                + extraAcceptancePercent
                + "% more are accepted."),
        new Benefit(
            "Verified-only trips open up",
            "Some drivers accept verified riders only. Those trips are hidden from everyone else."),
        new Benefit(
            "A badge on your profile",
            "Drivers can see a real person booked the seat, which is most of why they accept."));
  }
}
