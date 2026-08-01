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

  /**
   * The floor a fare may not price below. Base fare, per-km and per-minute were retired with the
   * old model in slice 03: a rate band prices the distance, and the platform's cut comes out of the
   * fare rather than being added to it.
   */
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
