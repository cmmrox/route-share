package com.routeshare.location.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;

public record LocationSampleRequest(
    @NotBlank String sampleId,
    @NotNull Instant capturedAt,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
    @NotNull @PositiveOrZero Double accuracyMeters,
    @PositiveOrZero Double speedMps,
    @DecimalMin("0.0") @DecimalMax("360.0") Double bearingDegrees,
    @DecimalMin("0") @DecimalMax("100") Integer batteryPercent) {}
