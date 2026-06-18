package com.routeshare.notification.dto;

import java.time.Instant;

public record NotificationResponse(
    long id,
    String type,
    String title,
    String body,
    String dataJson,
    boolean read,
    Instant createdAt) {}
