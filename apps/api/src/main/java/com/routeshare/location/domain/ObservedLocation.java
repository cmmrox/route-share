package com.routeshare.location.domain;

import java.time.Instant;

public record ObservedLocation(
    String sampleId,
    Instant capturedAt,
    double latitude,
    double longitude,
    double accuracyMeters,
    Double speedMps,
    Double bearingDegrees,
    Integer batteryPercent) {}
