package com.routeshare.location.dto.response;

import java.time.Instant;

public record AdminLiveTripResponse(
    Long tripId,
    Long driverProfileId,
    String driverName,
    String tripStatus,
    String originLabel,
    String destinationLabel,
    Instant departureTime,
    LocationSnapshotResponse latestLocation) {}
