package com.routeshare.admin.dto;

import java.time.Instant;

public record UserStatusHistoryResponse(
    long id,
    String fromStatus,
    String toStatus,
    String reason,
    Long changedBy,
    Instant createdAt) {}
