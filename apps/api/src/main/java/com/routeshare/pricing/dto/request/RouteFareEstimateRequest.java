package com.routeshare.pricing.dto.request;

import jakarta.validation.constraints.NotNull;

/** Fare estimate from pickup/drop-off coordinates; distance + duration are resolved server-side. */
public record RouteFareEstimateRequest(
    @NotNull Double pickupLat,
    @NotNull Double pickupLng,
    @NotNull Double dropoffLat,
    @NotNull Double dropoffLng) {}
