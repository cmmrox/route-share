package com.routeshare.safety.dto;

import java.time.Instant;

public record SosEventResponse(
    long id,
    String status,
    Long tripId,
    Long bookingId,
    Double latitude,
    Double longitude,
    String note,
    Instant createdAt) {}
