package com.routeshare.routing.entity;

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
 * A place a rider can actually be found.
 *
 * <p>A coordinate is not an instruction. In Colombo a 50 m GPS error puts the pin on the wrong side
 * of Galle Road, and no amount of matching accuracy helps because the error is in the pin. What
 * closes the gap is a name and a sentence: <em>the Rajagiriya junction bus halt, not the
 * roundabout</em>.
 *
 * <p>The geometry lives on this row but is never mapped here — every read goes through the
 * repository's projections, which return latitude and longitude as plain numbers rather than a
 * PostGIS type the rest of the application would then have to understand.
 */
@Entity
@Table(name = "pickup_point", schema = "routing")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PickupPointEntity {

  /** Operator-maintained. Wins over everything, and is where real landmark names come from. */
  public static final String SOURCE_CURATED = "CURATED";

  /** Resolved once from Places and then reused for ever. The launch default. */
  public static final String SOURCE_DERIVED = "DERIVED";

  /** Promoted later from {@code success_count}, once there is usage data to promote on. */
  public static final String SOURCE_LEARNED = "LEARNED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "pickup_point_id")
  private Long id;

  @Column(nullable = false)
  private String label;

  @Column private String description;

  /** "Kerb side, opposite the pharmacy" — the half of the instruction a coordinate cannot carry. */
  @Column(name = "side_hint")
  private String sideHint;

  @Column(nullable = false)
  private String source;

  @Column(name = "google_place_id")
  private String googlePlaceId;

  @Column(name = "success_count", nullable = false)
  private int successCount;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by_app_user_id")
  private Long createdByAppUserId;
}
