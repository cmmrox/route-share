package com.routeshare.routing.dto.response;

import java.util.List;

/**
 * P04's list, and the card that explains what is missing from it.
 *
 * <p>{@code filteredOutByRadius} exists because a silently short list reads as "no drivers", which
 * is a different and much worse message than "6 more drivers start further than 20 km away". It is
 * computed in the same statement as the results: two round trips and the two numbers eventually
 * disagree, and the one that is wrong is the one nobody checks.
 */
public record RideSearchPageResponse(
    List<RouteSearchResponse> results,
    /** Candidates on this corridor before the radius was applied. */
    long totalMatching,
    /** How many of those the radius removed. P04 shows this as its own card. */
    long filteredOutByRadius,
    int radiusKm,
    int maxRadiusKm,
    List<Integer> radiusOptionsKm,
    String sort,
    int page,
    int size,
    boolean hasMore) {}
