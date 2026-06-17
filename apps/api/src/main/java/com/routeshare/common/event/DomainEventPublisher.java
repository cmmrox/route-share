package com.routeshare.common.event;

/**
 * Transactional outbox publisher. Implementations persist the event in the same database
 * transaction as the caller's state change; a separate relay forwards it to the event stream. Call
 * this from inside a {@code @Transactional} service method.
 */
public interface DomainEventPublisher {
  void publish(DomainEvent event);
}
