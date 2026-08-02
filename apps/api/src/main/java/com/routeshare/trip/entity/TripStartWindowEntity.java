package com.routeshare.trip.entity;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The start-buffer clock (D32, D32c, D32b, P24, P35).
 *
 * <p>Runs from the trip's departure time and protects the <em>driver</em> from auto-cancellation.
 * It is deliberately not the same clock as the driver-late grace, which runs from a passenger's
 * promised pickup time and protects <em>her</em>: a trip that left on time can still be twenty
 * minutes from her corner. P35 exists to say so.
 */
@Entity
@Table(name = "trip_start_window", schema = "trip")
@Getter
@NoArgsConstructor
public class TripStartWindowEntity {
  public static final String RESOLUTION_STARTED = "STARTED";
  public static final String RESOLUTION_AUTO_CANCELLED = "AUTO_CANCELLED";
  public static final String RESOLUTION_CANCELLED = "CANCELLED";

  @Id
  @Column(name = "trip_id")
  private Long tripId;

  @Column(name = "departs_at", nullable = false)
  private Instant departsAt;

  @Column(name = "buffer_expires_at", nullable = false)
  private Instant bufferExpiresAt;

  @Column(name = "extension_used", nullable = false)
  private boolean extensionUsed;

  @Column(name = "extended_expires_at")
  private Instant extendedExpiresAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column private String resolution;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static TripStartWindowEntity opening(long tripId, Instant departsAt, Duration buffer) {
    var entity = new TripStartWindowEntity();
    entity.tripId = tripId;
    entity.departsAt = departsAt;
    entity.bufferExpiresAt = departsAt.plus(buffer);
    return entity;
  }

  /** The deadline in force right now: the extension moved it, or it never did. */
  public Instant effectiveDeadline() {
    return extendedExpiresAt != null ? extendedExpiresAt : bufferExpiresAt;
  }

  public boolean isResolved() {
    return resolvedAt != null;
  }

  public boolean hasExtensionRemaining(int limit) {
    return !extensionUsed && limit > 0;
  }

  /**
   * Spends the single extension. Returns false rather than throwing when it is already gone, so the
   * caller can answer D32c's "Extension already used" as data instead of an error.
   */
  public boolean extend(Duration by, int limit) {
    if (isResolved() || !hasExtensionRemaining(limit)) {
      return false;
    }
    this.extensionUsed = true;
    this.extendedExpiresAt = bufferExpiresAt.plus(by);
    return true;
  }

  /** Idempotent: a window already resolved keeps its original outcome and timestamp. */
  public void resolve(String resolution, Instant at) {
    if (isResolved()) {
      return;
    }
    this.resolution = resolution;
    this.resolvedAt = at;
  }
}
