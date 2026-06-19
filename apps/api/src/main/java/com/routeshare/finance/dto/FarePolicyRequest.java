package com.routeshare.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record FarePolicyRequest(
    @NotBlank String name,
    @NotNull @PositiveOrZero BigDecimal baseFare,
    @NotNull @PositiveOrZero BigDecimal perKm,
    @PositiveOrZero BigDecimal perMin,
    @PositiveOrZero BigDecimal minFare,
    String currency,
    Boolean active) {}
