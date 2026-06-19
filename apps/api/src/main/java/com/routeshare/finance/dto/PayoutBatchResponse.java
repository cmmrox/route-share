package com.routeshare.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PayoutBatchResponse(
    long id,
    String status,
    BigDecimal totalAmount,
    String currency,
    String note,
    Instant createdAt,
    Instant paidAt,
    List<Item> items) {
  public record Item(long id, long driverAppUserId, BigDecimal amount, String currency) {}
}
