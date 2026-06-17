package com.routeshare.common.event.impl;

import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.event.entity.EventOutboxEntity;
import com.routeshare.common.event.repository.EventOutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists domain events to the transactional outbox. Joins the caller's transaction (mandatory) so
 * the event is committed atomically with the state change it describes. Duplicate idempotency keys
 * are ignored, making re-publishing safe.
 */
@Component
public class OutboxDomainEventPublisher implements DomainEventPublisher {
  private final EventOutboxRepository outbox;

  public OutboxDomainEventPublisher(EventOutboxRepository outbox) {
    this.outbox = outbox;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void publish(DomainEvent event) {
    if (outbox.existsByIdempotencyKey(event.idempotencyKey())) {
      return;
    }
    outbox.save(
        EventOutboxEntity.from(
            event.eventType(),
            event.aggregateType(),
            event.aggregateId(),
            event.payloadJson(),
            event.idempotencyKey()));
  }
}
