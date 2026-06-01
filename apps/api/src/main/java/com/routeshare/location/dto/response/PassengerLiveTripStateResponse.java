package com.routeshare.location.dto.response;

import java.time.Instant;

public record PassengerLiveTripStateResponse(
    Long tripId,
    String tripStatus,
    String originLabel,
    String destinationLabel,
    Instant departureTime,
    LocationSnapshotResponse latestDriverLocation,
    boolean locationAvailable) {}
