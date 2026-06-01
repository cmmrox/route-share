package com.routeshare.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FareAdjustmentRequest(@NotNull BigDecimal amount, String reason) {}
