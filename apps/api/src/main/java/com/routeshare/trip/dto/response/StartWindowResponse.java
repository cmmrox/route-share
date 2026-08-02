package com.routeshare.trip.dto.response;

import java.time.Instant;

/**
 * D32 / D32c / D32b. The client renders a countdown and a consequence; it computes neither.
 *
 * @param extensionsRemaining 0 or 1 — D32c disables the button rather than failing on tap
 * @param missedStartsThisMonth what an expiry would make it
 * @param missedStartsBeforeDeactivation how many are left before driving stops
 */
public record StartWindowResponse(
    long tripId,
    Instant departsAt,
    Instant expiresAt,
    long secondsRemaining,
    int extensionsRemaining,
    int extensionMinutes,
    boolean expired,
    String resolution,
    int missedStartsThisMonth,
    int missedStartsBeforeDeactivation,
    String consequence) {}
