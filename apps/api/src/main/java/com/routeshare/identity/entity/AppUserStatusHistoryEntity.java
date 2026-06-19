package com.routeshare.identity.entity;

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
@Table(name = "app_user_status_history", schema = "identity")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUserStatusHistoryEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "app_user_status_history_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Column(name = "from_status")
  private String fromStatus;

  @Column(name = "to_status", nullable = false)
  private String toStatus;

  private String reason;

  @Column(name = "changed_by")
  private Long changedBy;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static AppUserStatusHistoryEntity of(
      long appUserId, String fromStatus, String toStatus, String reason, Long changedBy) {
    var e = new AppUserStatusHistoryEntity();
    e.appUserId = appUserId;
    e.fromStatus = fromStatus;
    e.toStatus = toStatus;
    e.reason = reason;
    e.changedBy = changedBy;
    return e;
  }
}
