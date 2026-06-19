package com.routeshare.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FinanceAdjustmentResponse(
    long id,
    Long bookingId,
    Long driverAppUserId,
    BigDecimal amount,
    String currency,
    String reason,
    Long createdBy,
    Instant createdAt) {}
