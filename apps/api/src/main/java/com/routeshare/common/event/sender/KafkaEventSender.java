package com.routeshare.common.event.sender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes relayed outbox events to Kafka/Redpanda. The producer is configured for idempotent,
 * {@code acks=all} delivery (see {@code spring.kafka.producer} in application.yml). Active only
 * when {@code routeshare.events.kafka-enabled=true}.
 */
@Component
@ConditionalOnProperty(prefix = "routeshare.events", name = "kafka-enabled", havingValue = "true")
public class KafkaEventSender implements EventSender {
  private final KafkaTemplate<Object, Object> kafkaTemplate;

  public KafkaEventSender(KafkaTemplate<Object, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @Override
  public void send(String topic, String key, String payload) {
    // Synchronous get() surfaces broker failures to the relay so the row is retried.
    try {
      kafkaTemplate.send(topic, key, payload).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while publishing event to Kafka", e);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to publish event to Kafka topic " + topic, e);
    }
  }
}
