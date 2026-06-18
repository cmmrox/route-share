package com.routeshare.notification.entity;

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

@Entity
@Table(name = "notification_delivery_log", schema = "notification")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDeliveryLogEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "notification_delivery_log_id")
  private Long id;

  @Column(name = "notification_id")
  private Long notificationId;

  @Column(nullable = false)
  private String channel;

  @Column(nullable = false)
  private String status;

  @Column(name = "provider_message_id")
  private String providerMessageId;

  private String error;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static NotificationDeliveryLogEntity of(
      Long notificationId, String channel, String status, String providerMessageId, String error) {
    var e = new NotificationDeliveryLogEntity();
    e.notificationId = notificationId;
    e.channel = channel;
    e.status = status;
    e.providerMessageId = providerMessageId;
    e.error = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
    return e;
  }
}
