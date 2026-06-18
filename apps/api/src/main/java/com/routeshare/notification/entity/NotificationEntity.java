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
@Table(name = "notification", schema = "notification")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "notification_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private String title;

  private String body;

  @Column(name = "data_json")
  private String dataJson;

  @Column(name = "read_at")
  private Instant readAt;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static NotificationEntity create(
      long appUserId, String type, String title, String body, String dataJson) {
    var e = new NotificationEntity();
    e.appUserId = appUserId;
    e.type = type;
    e.title = title;
    e.body = body;
    e.dataJson = dataJson;
    return e;
  }
}
