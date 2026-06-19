package com.routeshare.admin.dto;

import java.time.Instant;

public record AdminSosResponse(
    long id,
    long appUserId,
    String ownerRole,
    String status,
    Long tripId,
    Long bookingId,
    Double latitude,
    Double longitude,
    String note,
    Instant createdAt,
    Instant resolvedAt,
    Long resolvedBy,
    String resolutionNote) {}
