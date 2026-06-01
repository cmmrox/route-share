package com.routeshare.location.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;

public record DriverLocationUpdateRequest(
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
    @PositiveOrZero Double accuracyMeters,
    @PositiveOrZero Double speedMps,
    @DecimalMin("0.0") @DecimalMax("360.0") Double bearingDegrees,
    @NotNull Instant deviceRecordedAt,
    String devicePlatform,
    @DecimalMin("0.0") @DecimalMax("100.0") Double batteryPercent,
    String networkType) {}
