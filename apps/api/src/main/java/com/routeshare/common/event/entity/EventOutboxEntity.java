package com.routeshare.common.event.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event_outbox", schema = "common")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventOutboxEntity {
  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_SENT = "SENT";
  public static final String STATUS_FAILED = "FAILED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "event_outbox_id")
  private Long id;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "aggregate_type", nullable = false)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private String aggregateId;

  @Column(name = "payload_json", nullable = false)
  private String payloadJson;

  @Column(name = "idempotency_key", nullable = false, unique = true)
  private String idempotencyKey;

  @Column(nullable = false)
  private String status = STATUS_PENDING;

  @Column(nullable = false)
  private int attempts = 0;

  @Column(name = "last_error")
  private String lastError;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "sent_at")
  private Instant sentAt;

  public static EventOutboxEntity from(
      String eventType,
      String aggregateType,
      String aggregateId,
      String payloadJson,
      String idempotencyKey) {
    var entity = new EventOutboxEntity();
    entity.eventType = eventType;
    entity.aggregateType = aggregateType;
    entity.aggregateId = aggregateId;
    entity.payloadJson = payloadJson;
    entity.idempotencyKey = idempotencyKey;
    entity.status = STATUS_PENDING;
    entity.createdAt = Instant.now();
    return entity;
  }

  public void markSent(Instant when) {
    this.status = STATUS_SENT;
    this.sentAt = when;
    this.lastError = null;
  }

  public void markFailed(String error) {
    this.status = STATUS_FAILED;
    this.attempts += 1;
    this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
  }
}
