package com.routeshare.penalty.entity;

import com.routeshare.penalty.domain.PenaltyKind;
import com.routeshare.penalty.domain.PenaltySplit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One broken promise, priced.
 *
 * <p>The whole computation is stored, not just its result: the base it was taken from, the
 * percentage, and both halves. A penalty a support agent cannot explain in the caller's own numbers
 * is a refund, so "25% of LKR 197, LKR 25 to your driver" must be readable off the row.
 */
@Entity
@Table(name = "penalty_assessment", schema = "penalty")
@Getter
@NoArgsConstructor
public class PenaltyAssessmentEntity {
  public static final String STATUS_ASSESSED = "ASSESSED";
  public static final String STATUS_SETTLED = "SETTLED";
  public static final String STATUS_WAIVED = "WAIVED";
  public static final String STATUS_REVERSED = "REVERSED";

  public static final String COLLECTION_NETTED = "NETTED";
  public static final String COLLECTION_CARD_CHARGE = "CARD_CHARGE";
  public static final String COLLECTION_DUES = "DUES";
  public static final String COLLECTION_EARNINGS_DEDUCTION = "EARNINGS_DEDUCTION";
  public static final String COLLECTION_NONE = "NONE";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "penalty_id")
  private Long id;

  @Column(nullable = false)
  private String kind;

  @Column(name = "booking_id")
  private Long bookingId;

  @Column(name = "trip_id")
  private Long tripId;

  @Column(name = "payer_app_user_id", nullable = false)
  private Long payerAppUserId;

  @Column(name = "payer_role", nullable = false)
  private String payerRole;

  @Column(name = "victim_role", nullable = false)
  private String victimRole;

  @Column(name = "fare_base", nullable = false)
  private BigDecimal fareBase;

  @Column(name = "percent", nullable = false)
  private BigDecimal percent;

  @Column(name = "fee_amount", nullable = false)
  private BigDecimal feeAmount;

  @Column(name = "victim_share", nullable = false)
  private BigDecimal victimShare;

  @Column(name = "platform_share", nullable = false)
  private BigDecimal platformShare;

  @Column(nullable = false)
  private String status;

  @Column(name = "collection_method")
  private String collectionMethod;

  @Column(nullable = false)
  private String explanation;

  @Column(name = "assessed_at", nullable = false)
  private Instant assessedAt;

  @Column(name = "settled_at")
  private Instant settledAt;

  @Column(name = "policy_version", nullable = false)
  private String policyVersion;

  public static PenaltyAssessmentEntity of(
      PenaltyKind kind,
      Long bookingId,
      Long tripId,
      long payerAppUserId,
      BigDecimal fareBase,
      BigDecimal percent,
      PenaltySplit split,
      String explanation,
      Instant assessedAt,
      String policyVersion) {
    var entity = new PenaltyAssessmentEntity();
    entity.kind = kind.name();
    entity.bookingId = bookingId;
    entity.tripId = tripId;
    entity.payerAppUserId = payerAppUserId;
    entity.payerRole = kind.payerRole().name();
    entity.victimRole = kind.victimRole().name();
    entity.fareBase = fareBase;
    entity.percent = percent;
    entity.feeAmount = split.fee();
    entity.victimShare = split.victimShare();
    entity.platformShare = split.platformShare();
    entity.status = STATUS_ASSESSED;
    entity.explanation = explanation;
    entity.assessedAt = assessedAt;
    entity.policyVersion = policyVersion;
    return entity;
  }

  public PenaltyKind kindEnum() {
    return PenaltyKind.valueOf(kind);
  }

  public boolean isReversed() {
    return STATUS_REVERSED.equals(status);
  }

  /** Records how the money was taken and closes the assessment. */
  public void settle(String collectionMethod, Instant at) {
    this.collectionMethod = collectionMethod;
    this.status = STATUS_SETTLED;
    this.settledAt = at;
  }

  /** A zero-fee kind (D32b) is complete the moment it is written; nothing is collectable. */
  public void closeWithoutCollection(Instant at) {
    settle(COLLECTION_NONE, at);
  }

  /** An upheld dispute. The money goes back; the row stays, because the event still happened. */
  public void reverse(Instant at) {
    this.status = STATUS_REVERSED;
    this.settledAt = at;
  }
}
