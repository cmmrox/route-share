package com.routeshare.rewards.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record WithdrawalResponse(
    long id,
    BigDecimal amount,
    String status,
    Instant requestedAt,
    Instant batchedAt,
    Instant paidAt,
    String failureReason) {}
