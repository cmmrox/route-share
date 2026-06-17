package com.routeshare.common.event.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "routeshare.events")
public record EventProperties(
    boolean kafkaEnabled,
    String topicPrefix,
    Integer relayBatchSize,
    Long relayFixedDelayMs,
    Integer maxAttempts) {
  public EventProperties {
    topicPrefix = topicPrefix == null || topicPrefix.isBlank() ? "routeshare." : topicPrefix;
    relayBatchSize = relayBatchSize == null || relayBatchSize <= 0 ? 100 : relayBatchSize;
    relayFixedDelayMs =
        relayFixedDelayMs == null || relayFixedDelayMs <= 0 ? 2000L : relayFixedDelayMs;
    maxAttempts = maxAttempts == null || maxAttempts <= 0 ? 10 : maxAttempts;
  }

  /** Kafka topic name for an aggregate type, e.g. {@code routeshare.booking}. */
  public String topicFor(String aggregateType) {
    return topicPrefix + aggregateType;
  }
}
