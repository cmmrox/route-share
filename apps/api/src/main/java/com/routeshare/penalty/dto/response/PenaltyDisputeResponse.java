package com.routeshare.penalty.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PenaltyDisputeResponse(
    long id,
    long penaltyId,
    String kind,
    BigDecimal feeAmount,
    long raisedByAppUserId,
    String reason,
    String note,
    String status,
    Instant raisedAt,
    Instant decidedAt,
    String decisionNote,
    BigDecimal reversedAmount) {}
