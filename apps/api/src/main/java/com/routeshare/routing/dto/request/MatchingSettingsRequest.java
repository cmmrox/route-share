package com.routeshare.routing.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 20 km is a product ceiling with a reason — beyond it a driver is making a trip for the rider
 * rather than sharing one — so it is enforced on the way in as well as by the schema.
 */
public record MatchingSettingsRequest(
    @NotNull @Min(1000) @Max(20000) Integer defaultTripStartRadiusMeters,
    @NotNull @Min(1000) @Max(20000) Integer maxTripStartRadiusMeters,
    @NotEmpty List<@Min(1000) @Max(20000) Integer> allowedTripStartRadiiMeters,
    @NotNull @Min(5) @Max(1440) Integer defaultDepartureWindowMinutes,
    @NotNull @Min(5) @Max(1440) Integer maxDepartureWindowMinutes) {}
