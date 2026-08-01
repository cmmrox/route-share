package com.routeshare.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FarePolicyResponse(
    long id, String name, BigDecimal minFare, String currency, boolean active, Instant updatedAt) {}
