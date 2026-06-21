package com.routeshare.routing.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MatchingSettingsRequest(
    @NotNull @Min(100) @Max(20000) Integer defaultSearchRadiusMeters,
    @NotNull @Min(100) @Max(20000) Integer maxSearchRadiusMeters,
    @NotNull @Min(5) @Max(1440) Integer defaultDepartureWindowMinutes,
    @NotNull @Min(5) @Max(1440) Integer maxDepartureWindowMinutes) {}
