package com.routeshare.driver.entity;

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

/** D34's primary action: the driver asks to be reinstated, and an admin decides. */
@Entity
@Table(name = "driver_reinstatement_request", schema = "driver")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DriverReinstatementRequestEntity {
  public static final String STATUS_OPEN = "OPEN";
  public static final String STATUS_APPROVED = "APPROVED";
  public static final String STATUS_REJECTED = "REJECTED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "driver_reinstatement_request_id")
  private Long id;

  @Column(name = "driver_profile_id", nullable = false)
  private Long driverProfileId;

  @Column(name = "deactivation_id", nullable = false)
  private Long deactivationId;

  @Column(name = "support_ticket_id")
  private Long supportTicketId;

  private String message;

  @Column(nullable = false)
  private String status = STATUS_OPEN;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decided_by_app_user_id")
  private Long decidedByAppUserId;

  @Column(name = "decision_note")
  private String decisionNote;

  public static DriverReinstatementRequestEntity open(
      long driverProfileId, long deactivationId, String message, Long supportTicketId) {
    var e = new DriverReinstatementRequestEntity();
    e.driverProfileId = driverProfileId;
    e.deactivationId = deactivationId;
    e.message = message;
    e.supportTicketId = supportTicketId;
    e.status = STATUS_OPEN;
    return e;
  }

  public void decide(String status, Instant when, Long actorAppUserId, String note) {
    this.status = status;
    this.decidedAt = when;
    this.decidedByAppUserId = actorAppUserId;
    this.decisionNote = note;
  }
}
