package com.routeshare.platform.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "account_request", schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountRequestEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "account_request_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Column(nullable = false)
  private String kind;

  @Column(nullable = false)
  private String status;

  @Column(name = "requested_at", insertable = false, updatable = false)
  private Instant requestedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  private String note;

  public static AccountRequestEntity queued(long appUserId, String kind, String note) {
    var request = new AccountRequestEntity();
    request.appUserId = appUserId;
    request.kind = kind;
    request.status = "QUEUED";
    request.note = note;
    return request;
  }
}
