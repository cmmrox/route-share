package com.routeshare.admin.dto;

import java.time.Instant;

public record AdminTripResponse(
    long id,
    Long routePlanId,
    Long routeOccurrenceId,
    String status,
    Instant startedAt,
    Instant completedAt) {}
