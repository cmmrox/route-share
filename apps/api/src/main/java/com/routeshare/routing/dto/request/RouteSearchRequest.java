package com.routeshare.routing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * P03. What a rider is asking for, and how far she will let a driver's trip start from her.
 *
 * <p>{@code radiusKm} is measured from the driver's <b>trip origin</b>, not from how close his
 * route passes her pickup — a twentyfold change in magnitude on a different geometry, and the
 * reason the old {@code pickupRadiusMeters}/{@code dropoffRadiusMeters} pair is gone rather than
 * deprecated. Keeping them would leave two radii in one request with nothing to say which the query
 * honours. Pickup proximity is still scored; it is no longer the filter.
 *
 * <p>Only the offered radii are accepted, because those are the chips the screen shows and a rider
 * asking for 7 km is asking for something no screen can render.
 */
public record RouteSearchRequest(
    @NotNull @Valid CoordinateRequest pickup,
    @NotNull @Valid CoordinateRequest dropoff,
    @NotNull Instant requestedDepartureTime,
    @Min(1) int seats,
    /** One of the offered radii, in km. Null takes the configured default. */
    @Min(1) @Max(100) Integer radiusKm,
    @Min(1) @Max(1440) Integer departureWindowMinutes,
    /** {@code BEST_MATCH} (default), {@code CHEAPEST} or {@code SOONEST}. */
    String sort,
    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size) {}
