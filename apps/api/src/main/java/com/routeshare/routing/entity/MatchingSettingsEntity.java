package com.routeshare.routing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Singleton (id = 1) row holding admin-tunable route matching parameters. */
@Entity
@Table(name = "matching_settings", schema = "routing")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchingSettingsEntity {
  public static final int SINGLETON_ID = 1;

  @Id
  @Column(name = "matching_settings_id")
  private Integer id = SINGLETON_ID;

  /**
   * How far from the rider a driver's trip may <em>start</em> — slice 09's predicate, not the old
   * "how close does his route pass" one. Held here rather than in the policy table because this is
   * the row with an operator screen behind it, and one number with two homes is one number that
   * will eventually disagree with itself.
   */
  @Column(name = "default_trip_start_radius_m", nullable = false)
  private int defaultTripStartRadiusMeters;

  @Column(name = "max_trip_start_radius_m", nullable = false)
  private int maxTripStartRadiusMeters;

  /** The chips the screen offers. A rider asking for 7 km is asking for a screen that has no 7. */
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.ARRAY)
  @Column(name = "allowed_trip_start_radii_m", nullable = false)
  private int[] allowedTripStartRadiiMeters = {5_000, 10_000, 20_000};

  @Column(name = "default_departure_window_minutes", nullable = false)
  private int defaultDepartureWindowMinutes;

  @Column(name = "max_departure_window_minutes", nullable = false)
  private int maxDepartureWindowMinutes;

  @Column(name = "updated_at")
  private Instant updatedAt;

  public static MatchingSettingsEntity newSingleton() {
    var entity = new MatchingSettingsEntity();
    entity.id = SINGLETON_ID;
    return entity;
  }
}
