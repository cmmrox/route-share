package com.routeshare.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fare_policy", schema = "finance")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FarePolicyEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "fare_policy_id")
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(name = "base_fare", nullable = false)
  private BigDecimal baseFare;

  @Column(name = "per_km", nullable = false)
  private BigDecimal perKm;

  @Column(name = "per_min", nullable = false)
  private BigDecimal perMin = BigDecimal.ZERO;

  @Column(name = "min_fare", nullable = false)
  private BigDecimal minFare = BigDecimal.ZERO;

  @Column(nullable = false)
  private String currency = "LKR";

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public static FarePolicyEntity blank() {
    var e = new FarePolicyEntity();
    e.updatedAt = Instant.now();
    return e;
  }
}
