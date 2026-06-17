package com.routeshare.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Records processed provider webhook events for idempotency and audit. */
@Entity
@Table(name = "payment_webhook_event", schema = "payment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentWebhookEventEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_webhook_event_id")
  private Long id;

  @Column(nullable = false)
  private String provider;

  @Column(name = "event_id", nullable = false)
  private String eventId;

  @Column(name = "event_type")
  private String eventType;

  @Column(name = "received_at", insertable = false, updatable = false)
  private Instant receivedAt;

  public static PaymentWebhookEventEntity of(String provider, String eventId, String eventType) {
    var e = new PaymentWebhookEventEntity();
    e.provider = provider;
    e.eventId = eventId;
    e.eventType = eventType;
    return e;
  }
}
