package com.routeshare.routing.dto.response;

import java.util.List;

/**
 * The operator's view of the search rules.
 *
 * <p>The radius is expressed as trip-start distance from slice 09 onward, and as the set the screen
 * offers rather than a free range — P03 shows three chips, and a value between them could never be
 * selected.
 */
public record MatchingSettingsResponse(
    int defaultTripStartRadiusMeters,
    int maxTripStartRadiusMeters,
    List<Integer> allowedTripStartRadiiMeters,
    int defaultDepartureWindowMinutes,
    int maxDepartureWindowMinutes) {}
