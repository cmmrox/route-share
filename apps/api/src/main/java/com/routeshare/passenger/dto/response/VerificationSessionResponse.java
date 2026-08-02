package com.routeshare.passenger.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * P29 — the capture session and its four steps, in order.
 *
 * <p>The steps carry their own label, hint and guide shape so the copy a rider reads and the step a
 * reviewer decides on cannot drift apart.
 */
public record VerificationSessionResponse(
    long sessionId,
    String status,
    Instant expiresAt,
    long expiresInSeconds,
    boolean cameraOnly,
    List<Step> steps) {

  public record Step(
      String key,
      String label,
      String hint,
      String guideShape,
      String status,
      String rejectionReason) {}
}
