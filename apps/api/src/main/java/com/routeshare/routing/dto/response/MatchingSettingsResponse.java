package com.routeshare.routing.dto.response;

public record MatchingSettingsResponse(
    int defaultSearchRadiusMeters,
    int maxSearchRadiusMeters,
    int defaultDepartureWindowMinutes,
    int maxDepartureWindowMinutes) {}
