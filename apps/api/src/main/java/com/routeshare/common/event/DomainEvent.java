package com.routeshare.common.event;

import java.util.UUID;

/**
 * Immutable description of something that happened in a domain module. Events are persisted to
 * {@code common.event_outbox} in the same transaction as the state change and relayed to the event
 * stream asynchronously, giving an at-least-once, transactionally-consistent pipeline.
 *
 * @param eventType dotted event name, e.g. {@code booking.created}
 * @param aggregateType owning aggregate, e.g. {@code booking}
 * @param aggregateId business id of the aggregate as a string
 * @param payloadJson serialized JSON payload
 * @param idempotencyKey globally-unique key used to de-duplicate producers and consumers
 */
public record DomainEvent(
    String eventType,
    String aggregateType,
    String aggregateId,
    String payloadJson,
    String idempotencyKey) {

  public DomainEvent {
    if (eventType == null || eventType.isBlank()) {
      throw new IllegalArgumentException("eventType is required");
    }
    if (aggregateType == null || aggregateType.isBlank()) {
      throw new IllegalArgumentException("aggregateType is required");
    }
    if (aggregateId == null || aggregateId.isBlank()) {
      throw new IllegalArgumentException("aggregateId is required");
    }
    payloadJson = payloadJson == null ? "{}" : payloadJson;
    idempotencyKey =
        idempotencyKey == null || idempotencyKey.isBlank()
            ? eventType + ":" + aggregateId + ":" + UUID.randomUUID()
            : idempotencyKey;
  }

  /** Convenience factory that generates a random idempotency key. */
  public static DomainEvent of(
      String eventType, String aggregateType, String aggregateId, String payloadJson) {
    return new DomainEvent(eventType, aggregateType, aggregateId, payloadJson, null);
  }
}
