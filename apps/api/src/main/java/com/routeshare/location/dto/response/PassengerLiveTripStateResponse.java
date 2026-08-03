package com.routeshare.location.dto.response;

import com.routeshare.location.domain.LocationConfidence;
import java.time.Instant;

public record PassengerLiveTripStateResponse(
    Long tripId,
    String tripStatus,
    String originLabel,
    String destinationLabel,
    Instant departureTime,
    LocationSnapshotResponse latestDriverLocation,
    boolean locationAvailable,
    LocationConfidence confidence,
    Long updatedSecondsAgo) {}
