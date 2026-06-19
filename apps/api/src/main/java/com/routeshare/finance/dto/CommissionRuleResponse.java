package com.routeshare.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CommissionRuleResponse(
    long id, String scope, String scopeRef, BigDecimal rate, boolean active, Instant updatedAt) {}
