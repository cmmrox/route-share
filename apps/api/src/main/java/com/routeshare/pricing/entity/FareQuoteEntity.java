package com.routeshare.pricing.entity;

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
 * A fare as it was actually charged.
 *
 * <p>Persisted rather than recomputed, and never rewritten: rate bands move, discount tiers are
 * tunable, and a receipt read three months later must show the fare that was charged at the rate
 * that was then in force. Recomputing would quietly rewrite history.
 */
@Entity
@Table(name = "fare_quote", schema = "pricing")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FareQuoteEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "fare_quote_id")
  private Long id;

  @Column(name = "booking_id")
  private Long bookingId;

  @Column(name = "route_occurrence_id")
  private Long routeOccurrenceId;

  @Column(name = "vehicle_id")
  private Long vehicleId;

  @Column(name = "passenger_app_user_id")
  private Long passengerAppUserId;

  @Column(name = "on_route_distance_m", nullable = false)
  private BigDecimal onRouteDistanceMeters;

  @Column(name = "rate_per_km", nullable = false)
  private BigDecimal ratePerKm;

  @Column(nullable = false)
  private Integer seats;

  @Column(name = "gross_fare", nullable = false)
  private BigDecimal grossFare;

  @Column(name = "match_percent", nullable = false)
  private BigDecimal matchPercent;

  @Column(name = "match_tier", nullable = false)
  private String matchTier;

  @Column(name = "discount_percent", nullable = false)
  private BigDecimal discountPercent;

  @Column(name = "discount_amount", nullable = false)
  private BigDecimal discountAmount;

  @Column(name = "passenger_pays", nullable = false)
  private BigDecimal passengerPays;

  @Column(name = "commission_percent", nullable = false)
  private BigDecimal commissionPercent;

  @Column(name = "commission_amount", nullable = false)
  private BigDecimal commissionAmount;

  @Column(name = "driver_net", nullable = false)
  private BigDecimal driverNet;

  @Column(name = "min_fare_applied", nullable = false)
  private Boolean minFareApplied = false;

  @Column(nullable = false)
  private String currency = "LKR";

  @Column(name = "quoted_at", insertable = false, updatable = false)
  private Instant quotedAt;

  @Column(name = "policy_version")
  private String policyVersion;

  /**
   * Empty shell for the facade to fill; the fields are set from a computed quote, never by hand.
   */
  public static FareQuoteEntity blank() {
    return new FareQuoteEntity();
  }
}
