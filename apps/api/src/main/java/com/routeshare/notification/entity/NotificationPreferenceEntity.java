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
  @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
  @Column(name = "notification_preference_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Column(name = "category_key", nullable = false)
  private String categoryKey;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(nullable = false)
  private boolean push = true;

  @Column(nullable = false)
  private boolean sms;

  @Column(name = "in_app", nullable = false)
  private boolean inApp = true;

  @Column(nullable = false)
  private boolean mandatory;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public static NotificationPreferenceEntity defaultsFor(
      long appUserId,
      String categoryKey,
      boolean enabled,
      boolean push,
      boolean sms,
      boolean mandatory) {
    var e = new NotificationPreferenceEntity();
    e.appUserId = appUserId;
    e.categoryKey = categoryKey;
    e.enabled = enabled;
    e.push = push;
    e.sms = sms;
    e.inApp = true;
    e.mandatory = mandatory;
    return e;
  }
}
