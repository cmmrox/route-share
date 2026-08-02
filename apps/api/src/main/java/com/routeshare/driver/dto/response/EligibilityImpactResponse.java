package com.routeshare.driver.dto.response;

/**
 * D35's cost line — "verified riders only cost you 3 requests last week" — and the size of the pool
 * the toggle is drawn from.
 *
 * <p>Both are real counts rather than estimates, because a driver deciding whether to keep a safety
 * setting deserves to know what it actually costs him rather than what it might.
 */
public record EligibilityImpactResponse(
    int windowDays,
    long requestsTurnedAwayByVerifiedOnly,
    int verifiedRiderSharePercent,
    String summary) {}
