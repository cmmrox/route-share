package com.routeshare.pricing.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * An estimate for a specific published trip.
 *
 * <p>The fractions locate the rider's pickup and drop-off along the driver's stored route line; the
 * distance itself is measured server-side from that line, and the rate comes from the vehicle's
 * assessed band. Nothing here lets a caller name a distance, a rate or a fare.
 */
public record RouteFareEstimateRequest(
    @NotNull Long routeOccurrenceId,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double pickupRouteFraction,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double dropoffRouteFraction,
    @Min(1) int seats) {}
