package com.routeshare.reliability.entity;

import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Append-only. Nothing updates or deletes a row here.
 *
 * <p>A counter incremented in place cannot answer "why am I one miss from deactivation?", and D34
 * has to show the three misses with their dates, routes and rider counts. It also cannot be
 * corrected: an operator who disagrees with a no-show must be able to add a {@code CORRECTION}
 * rather than quietly decrement a number nobody can audit.
 */
@Entity
@Table(name = "reliability_event", schema = "reliability")
@Getter
@NoArgsConstructor
public class ReliabilityEventEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "reliability_event_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReliabilityRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false)
  private ReliabilityEventType eventType;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "booking_id")
  private Long bookingId;

  @Column(name = "trip_id")
  private Long tripId;

  @Column(name = "metadata", columnDefinition = "jsonb")
  private String metadata;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static ReliabilityEventEntity of(
      long appUserId,
      ReliabilityRole role,
      ReliabilityEventType type,
      Instant occurredAt,
      Long bookingId,
      Long tripId,
      String metadata) {
    var entity = new ReliabilityEventEntity();
    entity.appUserId = appUserId;
    entity.role = role;
    entity.eventType = type;
    entity.occurredAt = occurredAt;
    entity.bookingId = bookingId;
    entity.tripId = tripId;
    entity.metadata = metadata;
    return entity;
  }
}
