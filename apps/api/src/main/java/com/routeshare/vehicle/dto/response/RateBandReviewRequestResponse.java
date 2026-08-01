package com.routeshare.vehicle.dto.response;

import java.time.Instant;

public record RateBandReviewRequestResponse(
    long requestId,
    long vehicleId,
    String reason,
    String note,
    String status,
    Instant requestedAt,
    Instant decidedAt,
    String decisionNote) {}
