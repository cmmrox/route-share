package com.routeshare.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FinanceAdjustmentRequest(
    Long bookingId,
    Long driverAppUserId,
    @NotNull BigDecimal amount,
    String currency,
    @NotBlank String reason) {}
