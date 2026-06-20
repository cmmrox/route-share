package com.routeshare.admin.dto;

import java.time.Instant;

public record AdminBookingStatusHistoryResponse(
    long id,
    String fromStatus,
    String toStatus,
    Long changedByAppUserId,
    String reason,
    Instant changedAt) {}
