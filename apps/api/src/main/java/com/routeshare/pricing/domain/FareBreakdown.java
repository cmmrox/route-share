package com.routeshare.pricing.domain;

import java.math.BigDecimal;

public record FareBreakdown(
    BigDecimal baseFare,
    BigDecimal distanceFare,
    BigDecimal timeFare,
    BigDecimal platformFee,
    BigDecimal totalFare,
    String currency) {}
