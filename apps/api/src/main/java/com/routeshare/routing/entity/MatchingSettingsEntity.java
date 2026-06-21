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

  @Column(name = "default_search_radius_meters", nullable = false)
  private int defaultSearchRadiusMeters;

  @Column(name = "max_search_radius_meters", nullable = false)
  private int maxSearchRadiusMeters;

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
