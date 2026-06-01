package com.routeshare.booking.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
    @NotNull Long routePlanId,
    @Min(1) int seats,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double pickupLat,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double pickupLng,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double dropLat,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double dropLng) {}
