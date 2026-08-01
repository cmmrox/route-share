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

/**
 * A driver stopped from driving while their rider account continues untouched (board D34). Closed
 * by setting {@code reinstatedAt}; a partial unique index keeps at most one open row per driver.
 */
@Entity
@Table(name = "driver_deactivation", schema = "driver")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DriverDeactivationEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "driver_deactivation_id")
  private Long id;

  @Column(name = "driver_profile_id", nullable = false)
  private Long driverProfileId;

  @Column(nullable = false)
  private String reason;

  @Column(name = "case_ref", nullable = false)
  private String caseRef;

  @Column(name = "deactivated_at", insertable = false, updatable = false)
  private Instant deactivatedAt;

  @Column(name = "deactivated_by_app_user_id")
  private Long deactivatedByAppUserId;

  @Column(name = "reinstated_at")
  private Instant reinstatedAt;

  @Column(name = "reinstated_by_app_user_id")
  private Long reinstatedByAppUserId;

  public static DriverDeactivationEntity open(
      long driverProfileId, String reason, String caseRef, Long actorAppUserId) {
    var e = new DriverDeactivationEntity();
    e.driverProfileId = driverProfileId;
    e.reason = reason;
    e.caseRef = caseRef;
    e.deactivatedByAppUserId = actorAppUserId;
    return e;
  }

  public void reinstate(Instant when, Long actorAppUserId) {
    this.reinstatedAt = when;
    this.reinstatedByAppUserId = actorAppUserId;
  }
}
