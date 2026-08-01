package com.routeshare.vehicle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A driver asking for their band to be looked at again. The band is reviewed, not negotiated: one
 * open request per vehicle, and the existing band stays live throughout (D39).
 */
@Entity
@Table(name = "rate_band_review_request", schema = "vehicle")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RateBandReviewRequestEntity {
  public static final String STATUS_OPEN = "OPEN";
  public static final String STATUS_APPROVED = "APPROVED";
  public static final String STATUS_REJECTED = "REJECTED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "rate_band_review_request_id")
  private Long id;

  @Column(name = "vehicle_id", nullable = false)
  private Long vehicleId;

  @Column(name = "requested_by_app_user_id")
  private Long requestedByAppUserId;

  @Column(nullable = false)
  private String reason;

  private String note;

  @Column(nullable = false)
  private String status = STATUS_OPEN;

  @Column(name = "requested_at", insertable = false, updatable = false)
  private Instant requestedAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decided_by_app_user_id")
  private Long decidedByAppUserId;

  @Column(name = "decision_note")
  private String decisionNote;

  public static RateBandReviewRequestEntity open(
      long vehicleId, Long requestedByAppUserId, String reason, String note) {
    var entity = new RateBandReviewRequestEntity();
    entity.vehicleId = vehicleId;
    entity.requestedByAppUserId = requestedByAppUserId;
    entity.reason = reason;
    entity.note = note;
    entity.status = STATUS_OPEN;
    return entity;
  }

  public void decide(String status, Instant when, Long actorAppUserId, String decisionNote) {
    this.status = status;
    this.decidedAt = when;
    this.decidedByAppUserId = actorAppUserId;
    this.decisionNote = decisionNote;
  }
}
