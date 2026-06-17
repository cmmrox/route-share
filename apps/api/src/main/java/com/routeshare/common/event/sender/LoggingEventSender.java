package com.routeshare.common.event.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fallback sender used when Kafka publishing is disabled. It logs the event so the outbox still
 * drains in local/dev environments without a broker.
 */
@Component
@ConditionalOnProperty(
    prefix = "routeshare.events",
    name = "kafka-enabled",
    havingValue = "false",
    matchIfMissing = true)
public class LoggingEventSender implements EventSender {
  private static final Logger log = LoggerFactory.getLogger(LoggingEventSender.class);

  @Override
  public void send(String topic, String key, String payload) {
    log.info("event_relay topic={} key={} (kafka disabled, logging only)", topic, key);
  }
}
