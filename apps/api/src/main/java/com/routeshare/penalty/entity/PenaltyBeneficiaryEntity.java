package com.routeshare.penalty.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One person's share of one penalty's victim half.
 *
 * <p>Several rows may belong to a single penalty (D31 shares a driver's fee "between them as ride
 * credit"). Their amounts must total the victim share exactly; a deferred constraint trigger in
 * {@code V033} is what enforces it, because a remainder dropped here is money destroyed.
 */
@Entity
@Table(name = "penalty_beneficiary", schema = "penalty")
@Getter
@NoArgsConstructor
public class PenaltyBeneficiaryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "penalty_beneficiary_id")
  private Long id;

  @Column(name = "penalty_id", nullable = false)
  private Long penaltyId;

  @Column(name = "beneficiary_app_user_id", nullable = false)
  private Long beneficiaryAppUserId;

  @Column(name = "booking_id")
  private Long bookingId;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(name = "credited_at")
  private Instant creditedAt;

  @Column(name = "credit_reference")
  private String creditReference;

  public static PenaltyBeneficiaryEntity of(
      long penaltyId, long beneficiaryAppUserId, Long bookingId, BigDecimal amount) {
    var entity = new PenaltyBeneficiaryEntity();
    entity.penaltyId = penaltyId;
    entity.beneficiaryAppUserId = beneficiaryAppUserId;
    entity.bookingId = bookingId;
    entity.amount = amount;
    return entity;
  }

  public void credited(Instant at, String reference) {
    this.creditedAt = at;
    this.creditReference = reference;
  }
}
