package com.routeshare.location.domain;

import java.time.Instant;

public record ProgressState(
    double routeFraction,
    LocationConfidence confidence,
    Instant matchedAt,
    Instant updatedAt,
    Double speedMps,
    Double bearingDegrees,
    Double latitude,
    Double longitude,
    Instant offRouteSince,
    Double reversalCandidateFraction,
    int reversalCandidateCount) {}
