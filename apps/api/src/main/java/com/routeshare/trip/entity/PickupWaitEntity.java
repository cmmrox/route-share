package com.routeshare.trip.entity;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The pickup-wait clock (D19, D19b, P38, P38b, D21, P27).
 *
 * <p>Runs from <em>detected</em> GPS arrival. D19 says the wait starts automatically on arrival,
 * and that wording is load-bearing: a driver-triggered clock lets a no-show be manufactured two
 * streets away, and a no-show costs the passenger money and her record.
 */
@Entity
@Table(name = "pickup_wait", schema = "trip")
@Getter
@NoArgsConstructor
public class PickupWaitEntity {
  public static final String RESOLUTION_BOARDED = "BOARDED";
  public static final String RESOLUTION_NO_SHOW = "NO_SHOW";
  public static final String RESOLUTION_CANCELLED = "CANCELLED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "pickup_wait_id")
  private Long id;

  @Column(name = "trip_id", nullable = false)
  private Long tripId;

  @Column(name = "booking_id", nullable = false)
  private Long bookingId;

  @Column(name = "arrived_at", nullable = false)
  private Instant arrivedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "extension_used", nullable = false)
  private boolean extensionUsed;

  @Column(name = "extended_expires_at")
  private Instant extendedExpiresAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column private String resolution;

  /**
   * The samples that triggered arrival. A no-show is a penalty, and a penalty whose trigger cannot
   * be re-examined is one support has to take on faith when the passenger disputes it.
   */
  @Column(name = "triggered_by_samples", columnDefinition = "jsonb")
  private String triggeredBySamples;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static PickupWaitEntity startedOnArrival(
      long tripId, long bookingId, Instant arrivedAt, Duration wait, String triggeredBySamples) {
    var entity = new PickupWaitEntity();
    entity.tripId = tripId;
    entity.bookingId = bookingId;
    entity.arrivedAt = arrivedAt;
    // From the arrival, not from the moment the row was written: a slow write must not shorten her
    // wait, and a delayed one must not lengthen it.
    entity.expiresAt = arrivedAt.plus(wait);
    entity.triggeredBySamples = triggeredBySamples;
    return entity;
  }

  public Instant effectiveDeadline() {
    return extendedExpiresAt != null ? extendedExpiresAt : expiresAt;
  }

  public boolean isResolved() {
    return resolvedAt != null;
  }

  public boolean hasExtensionRemaining(int limit) {
    return !extensionUsed && limit > 0;
  }

  /** Same shape as the start extension, and measured from the deadline for the same reason. */
  public boolean extend(Duration by, int limit) {
    if (isResolved() || !hasExtensionRemaining(limit)) {
      return false;
    }
    this.extensionUsed = true;
    this.extendedExpiresAt = expiresAt.plus(by);
    return true;
  }

  /** Idempotent: a wait already resolved keeps its original outcome and timestamp. */
  public void resolve(String resolution, Instant at) {
    if (isResolved()) {
      return;
    }
    this.resolution = resolution;
    this.resolvedAt = at;
  }
}
