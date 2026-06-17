package com.routeshare.common.event.sender;

/**
 * Transport for relayed outbox events. The default {@link LoggingEventSender} is used until a Kafka
 * broker is configured ({@code routeshare.events.kafka-enabled=true}), at which point {@link
 * KafkaEventSender} takes over.
 */
public interface EventSender {
  void send(String topic, String key, String payload);
}
