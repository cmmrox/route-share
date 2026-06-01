package com.routeshare.location.dto.response;

import java.time.Instant;

public record LocationUpdateResponse(
    boolean accepted,
    Long tripId,
    Long driverProfileId,
    Instant serverReceivedAt,
    LocationSnapshotResponse latestLocation) {}
