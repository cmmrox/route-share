package com.routeshare.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payout_batch", schema = "finance")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayoutBatchEntity {
  public static final String OPEN = "OPEN";
  public static final String PAID = "PAID";
  public static final String CANCELLED = "CANCELLED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payout_batch_id")
  private Long id;

  @Column(nullable = false)
  private String status = OPEN;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(nullable = false)
  private String currency = "LKR";

  private String note;

  @Column(name = "created_by")
  private Long createdBy;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "paid_at")
  private Instant paidAt;

  public static PayoutBatchEntity open(Long createdBy, String note) {
    var e = new PayoutBatchEntity();
    e.status = OPEN;
    e.createdBy = createdBy;
    e.note = note;
    e.totalAmount = BigDecimal.ZERO;
    return e;
  }
}
