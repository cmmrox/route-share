package com.routeshare.admin.dto;

import java.time.Instant;

public record AuditActionResponse(
    long id,
    Long actorAppUserId,
    String actorRole,
    String action,
    String targetType,
    String targetId,
    String detailJson,
    Instant createdAt) {}
