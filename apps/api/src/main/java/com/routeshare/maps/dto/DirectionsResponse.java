package com.routeshare.maps.dto;

import java.util.List;

public record DirectionsResponse(
    List<CoordinateResponse> coordinates,
    long distanceMeters,
    long durationSeconds,
    String source) {}
