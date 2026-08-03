package com.routeshare.safety.dto;

import jakarta.validation.constraints.Size;

public record RaiseSosRequest(
    @Size(max = 40) String kind,
    Long tripId,
    Long bookingId,
    Double latitude,
    Double longitude,
    @Size(max = 500) String note) {}
