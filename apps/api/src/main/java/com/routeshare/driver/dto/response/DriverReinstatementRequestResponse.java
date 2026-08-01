package com.routeshare.driver.dto.response;

import java.time.Instant;

public record DriverReinstatementRequestResponse(
    long requestId,
    long deactivationId,
    Long supportTicketId,
    String message,
    String status,
    Instant createdAt,
    Instant decidedAt,
    String decisionNote) {}
