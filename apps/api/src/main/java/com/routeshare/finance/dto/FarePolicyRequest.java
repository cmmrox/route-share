package com.routeshare.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Base fare, per-km and per-minute were retired with the old fare model (slice 03). */
public record FarePolicyRequest(
    @NotBlank String name, @PositiveOrZero BigDecimal minFare, String currency, Boolean active) {}
