package com.routeshare.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_preference", schema = "notification")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreferenceEntity {
  @Id
  @Column(name = "app_user_id")
  private Long appUserId;

  @Column(name = "push_enabled", nullable = false)
  private boolean pushEnabled = true;

  @Column(name = "email_enabled", nullable = false)
  private boolean emailEnabled = true;

  @Column(name = "booking_updates", nullable = false)
  private boolean bookingUpdates = true;

  @Column(name = "trip_updates", nullable = false)
  private boolean tripUpdates = true;

  @Column(name = "payment_updates", nullable = false)
  private boolean paymentUpdates = true;

  @Column(nullable = false)
  private boolean marketing = false;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public static NotificationPreferenceEntity defaultsFor(long appUserId) {
    var e = new NotificationPreferenceEntity();
    e.appUserId = appUserId;
    return e;
  }
}
