package com.routeshare.notification.dto;

import java.time.Instant;

public record NotificationResponse(
    long id,
    String type,
    String category,
    String title,
    String body,
    String dataJson,
    String actionPath,
    boolean deferred,
    boolean read,
    Instant createdAt) {}
