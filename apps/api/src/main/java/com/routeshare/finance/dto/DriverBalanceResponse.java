package com.routeshare.finance.dto;

import java.math.BigDecimal;

public record DriverBalanceResponse(
    long driverAppUserId,
    BigDecimal earned,
    BigDecimal paidOut,
    BigDecimal balance,
    String currency) {}
