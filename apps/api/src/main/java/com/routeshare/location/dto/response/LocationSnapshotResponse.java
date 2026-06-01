package com.routeshare.location.dto.response;

import java.time.Instant;

public record LocationSnapshotResponse(
    Long tripId,
    Long driverProfileId,
    Double latitude,
    Double longitude,
    Double accuracyMeters,
    Double speedMps,
    Double bearingDegrees,
    Instant deviceRecordedAt,
    Instant serverReceivedAt,
    boolean stale) {}
