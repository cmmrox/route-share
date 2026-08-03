package com.routeshare.chat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_admin_read_audit", schema = "chat")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatAdminReadAuditEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "chat_admin_read_audit_id")
  private Long id;

  @Column(name = "chat_thread_id", nullable = false)
  private Long threadId;

  @Column(name = "admin_app_user_id", nullable = false)
  private Long adminAppUserId;

  @Column(nullable = false)
  private String reason;

  @Column(name = "read_at", insertable = false, updatable = false)
  private Instant readAt;

  public static ChatAdminReadAuditEntity record(long threadId, long adminAppUserId, String reason) {
    var audit = new ChatAdminReadAuditEntity();
    audit.threadId = threadId;
    audit.adminAppUserId = adminAppUserId;
    audit.reason = reason;
    return audit;
  }
}
