package com.routeshare.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payout_batch_item", schema = "finance")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayoutBatchItemEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payout_batch_item_id")
  private Long id;

  @Column(name = "payout_batch_id", nullable = false)
  private Long payoutBatchId;

  @Column(name = "driver_app_user_id", nullable = false)
  private Long driverAppUserId;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private String currency = "LKR";

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static PayoutBatchItemEntity of(
      long payoutBatchId, long driverAppUserId, BigDecimal amount, String currency) {
    var e = new PayoutBatchItemEntity();
    e.payoutBatchId = payoutBatchId;
    e.driverAppUserId = driverAppUserId;
    e.amount = amount;
    e.currency = currency == null ? "LKR" : currency;
    return e;
  }
}
