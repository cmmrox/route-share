package com.routeshare.vehicle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reference data: what a class of vehicle may carry and the range its per-km band must sit inside.
 * Seeded by migration and read-only at runtime.
 */
@Entity
@Table(name = "vehicle_class", schema = "vehicle")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleClassEntity {
  @Id
  @Column(name = "class_key")
  private String classKey;

  @Column(nullable = false)
  private String label;

  @Column(name = "max_passenger_seats", nullable = false)
  private Integer maxPassengerSeats;

  @Column(name = "default_rate_min", nullable = false)
  private BigDecimal defaultRateMin;

  @Column(name = "default_rate_max", nullable = false)
  private BigDecimal defaultRateMax;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder;

  @Column(nullable = false)
  private Boolean active;

  public static VehicleClassEntity of(
      String classKey,
      String label,
      int maxPassengerSeats,
      BigDecimal defaultRateMin,
      BigDecimal defaultRateMax,
      int sortOrder) {
    var entity = new VehicleClassEntity();
    entity.classKey = classKey;
    entity.label = label;
    entity.maxPassengerSeats = maxPassengerSeats;
    entity.defaultRateMin = defaultRateMin;
    entity.defaultRateMax = defaultRateMax;
    entity.sortOrder = sortOrder;
    entity.active = true;
    return entity;
  }
}
