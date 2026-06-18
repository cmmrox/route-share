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
@Table(name = "push_registration", schema = "notification")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushRegistrationEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "push_registration_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Column(nullable = false)
  private String platform;

  @Column(nullable = false)
  private String token;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "last_seen_at", nullable = false)
  private Instant lastSeenAt = Instant.now();

  public static PushRegistrationEntity create(long appUserId, String platform, String token) {
    var e = new PushRegistrationEntity();
    e.appUserId = appUserId;
    e.platform = platform;
    e.token = token;
    e.enabled = true;
    e.lastSeenAt = Instant.now();
    return e;
  }
}
