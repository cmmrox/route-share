package com.routeshare.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminLocationSampleResponse(
    double latitude,
    double longitude,
    BigDecimal speedMps,
    BigDecimal bearingDegrees,
    BigDecimal accuracyMeters,
    Instant recordedAt) {}
