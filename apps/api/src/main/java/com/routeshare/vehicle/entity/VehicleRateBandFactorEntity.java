package com.routeshare.vehicle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One line of the arithmetic shown on D39 — "Insurance, comprehensive cover, +3".
 *
 * <p>Displayed justification, not an input: decision D2 chose admin-typed bands over a scoring
 * engine, so these rows explain a band that already exists rather than producing one.
 */
@Entity
@Table(name = "vehicle_rate_band_factor", schema = "vehicle")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleRateBandFactorEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "vehicle_rate_band_factor_id")
  private Long id;

  @Column(name = "vehicle_rate_band_id", nullable = false)
  private Long vehicleRateBandId;

  @Column(name = "factor_key", nullable = false)
  private String factorKey;

  @Column(nullable = false)
  private String label;

  private String detail;

  @Column(nullable = false)
  private BigDecimal delta;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  public static VehicleRateBandFactorEntity of(
      long bandId, String factorKey, String label, String detail, BigDecimal delta, int sortOrder) {
    var entity = new VehicleRateBandFactorEntity();
    entity.vehicleRateBandId = bandId;
    entity.factorKey = factorKey;
    entity.label = label;
    entity.detail = detail;
    entity.delta = delta;
    entity.sortOrder = sortOrder;
    return entity;
  }
}
