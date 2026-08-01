package com.routeshare.booking.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
    @NotNull Long routeOccurrenceId,
    @Min(1) int seats,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double pickupLat,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double pickupLng,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double dropLat,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double dropLng,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double pickupRouteFraction,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double dropoffRouteFraction,
    /**
     * The card to hold the fare on. Null means cash: nothing is authorised, and the driver collects
     * in the car (boards D23 and P09's cash variant).
     */
    Long paymentMethodId) {}
