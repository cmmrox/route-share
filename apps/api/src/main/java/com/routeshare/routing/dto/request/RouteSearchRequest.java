package com.routeshare.routing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record RouteSearchRequest(
    @NotNull @Valid CoordinateRequest pickup,
    @NotNull @Valid CoordinateRequest dropoff,
    @NotNull Instant requestedDepartureTime,
    @Min(1) int seats,
    @Min(1) @Max(10_000) Integer pickupRadiusMeters,
    @Min(1) @Max(10_000) Integer dropoffRadiusMeters,
    @Min(1) @Max(1440) Integer departureWindowMinutes,
    @Min(1) @Max(100) Integer limit) {}
