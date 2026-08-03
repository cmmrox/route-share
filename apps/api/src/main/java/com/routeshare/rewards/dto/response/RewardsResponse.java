package com.routeshare.rewards.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RewardsResponse(
    BigDecimal balance,
    BigDecimal bankMinimum,
    BigDecimal withdrawable,
    BigDecimal shortfall,
    boolean autoApply,
    BigDecimal referralEarned,
    int peopleStillEarning,
    List<Row> rows) {
  public record Row(
      long id, Instant occurredAt, String kind, String label, String sublabel, BigDecimal amount) {}
}
