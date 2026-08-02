package com.routeshare.trip.entity;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The driver-late grace (P34, D41, P35).
 *
 * <p>Runs from <em>this passenger's</em> promised pickup time and protects <em>her</em>. P35 exists
 * to say this is not the start buffer: that one runs from the trip's departure and protects him,
 * and a trip that left exactly on time can still be twenty minutes from her corner. His extension
 * is his protection, not an obligation on her.
 */
@Entity
@Table(name = "driver_late_grace", schema = "trip")
@Getter
@NoArgsConstructor
public class DriverLateGraceEntity {
  public static final String RESOLUTION_PICKED_UP = "PICKED_UP";
  public static final String RESOLUTION_FREE_CANCELLED = "FREE_CANCELLED";
  public static final String RESOLUTION_EXPIRED = "EXPIRED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "driver_late_grace_id")
  private Long id;

  @Column(name = "booking_id", nullable = false)
  private Long bookingId;

  @Column(name = "promised_pickup_at", nullable = false)
  private Instant promisedPickupAt;

  @Column(name = "grace_expires_at", nullable = false)
  private Instant graceExpiresAt;

  @Column(name = "unlocked_at")
  private Instant unlockedAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column private String resolution;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static DriverLateGraceEntity opening(
      long bookingId, Instant promisedPickupAt, Duration grace) {
    var entity = new DriverLateGraceEntity();
    entity.bookingId = bookingId;
    entity.promisedPickupAt = promisedPickupAt;
    entity.graceExpiresAt = promisedPickupAt.plus(grace);
    return entity;
  }

  public boolean isResolved() {
    return resolvedAt != null;
  }

  public boolean isUnlocked() {
    return unlockedAt != null;
  }

  /** Idempotent: a free cancel already unlocked keeps the instant it was unlocked at. */
  public void unlock(Instant at) {
    if (unlockedAt == null) {
      this.unlockedAt = at;
    }
  }

  public void resolve(String resolution, Instant at) {
    if (isResolved()) {
      return;
    }
    this.resolution = resolution;
    this.resolvedAt = at;
  }
}
