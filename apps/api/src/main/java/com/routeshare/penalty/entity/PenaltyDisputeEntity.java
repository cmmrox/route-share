package com.routeshare.penalty.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "I was there, he drove past." One open dispute per penalty — re-raising is the same argument, not
 * a second case, and a partial unique index in {@code V033} says so.
 */
@Entity
@Table(name = "penalty_dispute", schema = "penalty")
@Getter
@NoArgsConstructor
public class PenaltyDisputeEntity {
  public static final String STATUS_OPEN = "OPEN";
  public static final String STATUS_UPHELD = "UPHELD";
  public static final String STATUS_REVERSED = "REVERSED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "penalty_dispute_id")
  private Long id;

  @Column(name = "penalty_id", nullable = false)
  private Long penaltyId;

  @Column(name = "raised_by_app_user_id", nullable = false)
  private Long raisedByAppUserId;

  @Column(nullable = false)
  private String reason;

  @Column private String note;

  @Column(nullable = false)
  private String status;

  @Column(name = "raised_at", nullable = false)
  private Instant raisedAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decided_by_app_user_id")
  private Long decidedByAppUserId;

  @Column(name = "decision_note")
  private String decisionNote;

  @Column(name = "reversed_amount")
  private BigDecimal reversedAmount;

  public static PenaltyDisputeEntity opened(
      long penaltyId, long raisedByAppUserId, String reason, String note, Instant raisedAt) {
    var entity = new PenaltyDisputeEntity();
    entity.penaltyId = penaltyId;
    entity.raisedByAppUserId = raisedByAppUserId;
    entity.reason = reason;
    entity.note = note;
    entity.status = STATUS_OPEN;
    entity.raisedAt = raisedAt;
    return entity;
  }

  public boolean isOpen() {
    return STATUS_OPEN.equals(status);
  }

  public void decide(
      String status,
      long decidedByAppUserId,
      String decisionNote,
      BigDecimal reversedAmount,
      Instant at) {
    this.status = status;
    this.decidedByAppUserId = decidedByAppUserId;
    this.decisionNote = decisionNote;
    this.reversedAmount = reversedAmount;
    this.decidedAt = at;
  }
}
