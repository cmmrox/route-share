package com.routeshare.vehicle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The band ComiGo assessed for one vehicle, and the rate its driver chose inside it.
 *
 * <p>The band's lifecycle is its own, separate from vehicle approval: approved papers do not make a
 * vehicle publishable, because without a band there is no legal price to put on a seat (board D40).
 */
@Entity
@Table(name = "vehicle_rate_band", schema = "vehicle")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleRateBandEntity {
  public static final String STATUS_NOT_SET = "NOT_SET";
  public static final String STATUS_PENDING_ASSESSMENT = "PENDING_ASSESSMENT";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "vehicle_rate_band_id")
  private Long id;

  @Column(name = "vehicle_id", nullable = false)
  private Long vehicleId;

  @Column(name = "rate_min", nullable = false)
  private BigDecimal rateMin;

  @Column(name = "rate_max", nullable = false)
  private BigDecimal rateMax;

  @Column(name = "chosen_rate")
  private BigDecimal chosenRate;

  @Column(nullable = false)
  private String status = STATUS_PENDING_ASSESSMENT;

  @Column(name = "set_by_app_user_id")
  private Long setByAppUserId;

  @Column(name = "set_at")
  private Instant setAt;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false)
  private Instant updatedAt;

  /** The row D40 renders: a vehicle waiting for its first assessment. */
  public static VehicleRateBandEntity pendingAssessment(
      long vehicleId, BigDecimal classMin, BigDecimal classMax) {
    var entity = new VehicleRateBandEntity();
    entity.vehicleId = vehicleId;
    entity.rateMin = classMin;
    entity.rateMax = classMax;
    entity.status = STATUS_PENDING_ASSESSMENT;
    return entity;
  }

  public boolean isActive() {
    return STATUS_ACTIVE.equals(status) || STATUS_UNDER_REVIEW.equals(status);
  }
}
