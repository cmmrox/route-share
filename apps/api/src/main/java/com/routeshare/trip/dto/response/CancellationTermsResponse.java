package com.routeshare.trip.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The single source both P26 and P34 read. The client never decides whether a cancel is free.
 *
 * <p>Two screens asking the same question of two different code paths is how they come to disagree,
 * and the disagreement is only discovered when somebody is charged a fee the screen said was
 * waived.
 *
 * @param free whether cancelling right now costs her nothing
 * @param reasonCode why, as a stable code the client maps to copy
 * @param penaltyPct what would apply if she cancels anyway
 * @param penaltyAmount the same rule, priced — so P26 states a figure rather than asking her to
 *     work out 20% of a fare she is looking at on another screen
 * @param penaltyVictimShare her driver's half of it, which P26 names explicitly
 * @param penaltyPlatformShare ComiGo's half; the two always re-add to {@code penaltyAmount}
 * @param recordedAgainstPassenger whether this cancel goes on her record — a free cancel unlocked
 *     by a late driver does not
 */
public record CancellationTermsResponse(
    long bookingId,
    boolean free,
    String reasonCode,
    String explanation,
    BigDecimal penaltyPct,
    BigDecimal fareBase,
    BigDecimal penaltyAmount,
    BigDecimal penaltyVictimShare,
    BigDecimal penaltyPlatformShare,
    boolean recordedAgainstPassenger,
    Instant promisedPickupAt,
    Instant freeCancelUnlockedAt,
    Instant freeCancelUnlocksAt,
    Long secondsUntilFreeCancel) {}
