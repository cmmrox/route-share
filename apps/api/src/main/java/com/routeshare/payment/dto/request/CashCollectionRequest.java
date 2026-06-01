package com.routeshare.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CashCollectionRequest(
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount, String note) {}
