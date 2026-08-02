package com.routeshare.trip.dto.response;

import java.time.Instant;

/**
 * D19 / D19b from the driver's side, and P38 / P38b from hers — the same clock, described so that
 * neither client has to know a policy figure to render it.
 *
 * @param extensionsRemaining 0 or 1; D19b replaces the button rather than failing on tap
 * @param releasableNow whether the seat may be released yet. A driver must not be able to
 *     manufacture a no-show early, so this is server-decided and the button follows it.
 * @param consequence what happens at zero, in words the screen can show unchanged
 */
public record PickupWaitResponse(
    long tripId,
    long bookingId,
    Instant arrivedAt,
    Instant expiresAt,
    long secondsRemaining,
    int extensionsRemaining,
    int extensionMinutes,
    boolean expired,
    boolean releasableNow,
    String resolution,
    int noShowsThisMonth,
    int prepayThreshold,
    String consequence) {}
