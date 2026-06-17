package com.routeshare.common.event.relay;

import com.routeshare.common.event.config.EventProperties;
import com.routeshare.common.event.entity.EventOutboxEntity;
import com.routeshare.common.event.repository.EventOutboxRepository;
import com.routeshare.common.event.sender.EventSender;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drains {@code common.event_outbox} and forwards each event through the active {@link
 * EventSender}. Rows are claimed with {@code FOR UPDATE SKIP LOCKED} so the relay is safe to run on
 * every application instance. A successful send marks the row {@code SENT}; a failure marks it
 * {@code FAILED} and increments the attempt counter for bounded retry.
 */
@Component
public class OutboxRelayScheduler {
  private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

  private final EventOutboxRepository outbox;
  private final EventSender sender;
  private final EventProperties properties;
  private final Clock clock;

  public OutboxRelayScheduler(
      EventOutboxRepository outbox, EventSender sender, EventProperties properties, Clock clock) {
    this.outbox = outbox;
    this.sender = sender;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${routeshare.events.relay-fixed-delay-ms:2000}")
  @Transactional
  public void relay() {
    var batch =
        outbox.claimDispatchable(
            properties.maxAttempts(), PageRequest.of(0, properties.relayBatchSize()));
    if (batch.isEmpty()) {
      return;
    }
    for (EventOutboxEntity row : batch) {
      try {
        sender.send(
            properties.topicFor(row.getAggregateType()),
            row.getAggregateId(),
            row.getPayloadJson());
        row.markSent(Instant.now(clock));
      } catch (RuntimeException e) {
        log.warn("event_relay_failed id={} type={}", row.getId(), row.getEventType(), e);
        row.markFailed(e.getMessage());
      }
    }
  }
}
