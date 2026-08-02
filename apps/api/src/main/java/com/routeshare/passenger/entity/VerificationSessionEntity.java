package com.routeshare.passenger.entity;

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
 * One attempt at proving who a rider is: four captures a reviewer decides on together.
 *
 * <p>The session exists so each upload can be bound to a short-lived, server-issued id. Without it,
 * "this came from the camera" is a claim attached to nothing, and a rider could assemble the four
 * images at leisure — which is exactly what the selfie-with-NIC is there to rule out.
 */
@Entity
@Table(name = "verification_session", schema = "passenger")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationSessionEntity {

  public static final String OPEN = "OPEN";
  public static final String SUBMITTED = "SUBMITTED";
  public static final String APPROVED = "APPROVED";
  public static final String REJECTED = "REJECTED";
  public static final String EXPIRED = "EXPIRED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "verification_session_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Column(nullable = false)
  private String status = OPEN;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decided_by_app_user_id")
  private Long decidedByAppUserId;

  @Column(name = "decision_note")
  private String decisionNote;

  public static VerificationSessionEntity open(long appUserId, Instant expiresAt) {
    var e = new VerificationSessionEntity();
    e.appUserId = appUserId;
    e.status = OPEN;
    e.expiresAt = expiresAt;
    return e;
  }

  /**
   * A session is expired by the clock, not by a sweeper. Storing a boolean would leave every path
   * that forgot to run the sweep accepting captures into a session that lapsed hours ago.
   */
  public boolean hasLapsed(Instant now) {
    return OPEN.equals(status) && !now.isBefore(expiresAt);
  }

  public void markSubmitted(Instant when) {
    this.status = SUBMITTED;
    this.submittedAt = when;
  }

  public void decide(String outcome, long reviewerAppUserId, String note, Instant when) {
    this.status = outcome;
    this.decidedByAppUserId = reviewerAppUserId;
    this.decisionNote = note;
    this.decidedAt = when;
  }
}
