package com.routeshare.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.event.entity.EventOutboxEntity;
import com.routeshare.common.event.impl.OutboxDomainEventPublisher;
import com.routeshare.common.event.repository.EventOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxDomainEventPublisherTest {

  @Mock private EventOutboxRepository outbox;

  @Test
  void persistsOutboxRowForNewEvent() {
    var publisher = new OutboxDomainEventPublisher(outbox);
    when(outbox.existsByIdempotencyKey("k1")).thenReturn(false);

    publisher.publish(new DomainEvent("booking.created", "booking", "42", "{\"id\":42}", "k1"));

    ArgumentCaptor<EventOutboxEntity> captor = ArgumentCaptor.forClass(EventOutboxEntity.class);
    verify(outbox).save(captor.capture());
    EventOutboxEntity saved = captor.getValue();
    assertThat(saved.getEventType()).isEqualTo("booking.created");
    assertThat(saved.getAggregateType()).isEqualTo("booking");
    assertThat(saved.getAggregateId()).isEqualTo("42");
    assertThat(saved.getIdempotencyKey()).isEqualTo("k1");
    assertThat(saved.getStatus()).isEqualTo(EventOutboxEntity.STATUS_PENDING);
  }

  @Test
  void skipsDuplicateIdempotencyKey() {
    var publisher = new OutboxDomainEventPublisher(outbox);
    when(outbox.existsByIdempotencyKey("dup")).thenReturn(true);

    publisher.publish(new DomainEvent("booking.created", "booking", "42", "{}", "dup"));

    verify(outbox, never()).save(any());
  }

  @Test
  void generatesIdempotencyKeyWhenMissing() {
    var event = DomainEvent.of("trip.started", "trip", "7", "{}");
    assertThat(event.idempotencyKey()).startsWith("trip.started:7:");
  }
}
