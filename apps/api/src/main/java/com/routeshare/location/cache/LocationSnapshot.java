package com.routeshare.location.cache;

import com.routeshare.location.dto.response.LocationSnapshotResponse;
import java.time.Duration;
import java.time.Instant;

public record LocationSnapshot(
    Long tripId,
    Long driverProfileId,
    Double latitude,
    Double longitude,
    Double accuracyMeters,
    Double speedMps,
    Double bearingDegrees,
    Instant deviceRecordedAt,
    Instant serverReceivedAt) {
  public LocationSnapshotResponse toResponse(Instant now, Duration ttl) {
    boolean stale = serverReceivedAt == null || serverReceivedAt.plus(ttl).isBefore(now);
    return new LocationSnapshotResponse(
        tripId,
        driverProfileId,
        latitude,
        longitude,
        accuracyMeters,
        speedMps,
        bearingDegrees,
        deviceRecordedAt,
        serverReceivedAt,
        stale);
  }
}
