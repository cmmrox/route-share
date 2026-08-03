package com.routeshare.location.dto.response;

import com.routeshare.location.domain.LocationConfidence;
import java.time.Instant;

public record TripProgressResponse(
    long tripId,
    double routeFraction,
    LocationConfidence confidence,
    Instant matchedAt,
    long updatedSecondsAgo,
    Double speedMps,
    Double bearingDegrees,
    boolean offRoute,
    double remainingDistanceMeters,
    long etaSeconds) {}
