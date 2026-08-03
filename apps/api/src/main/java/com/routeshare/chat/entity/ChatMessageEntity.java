package com.routeshare.chat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_message", schema = "chat")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "chat_message_id")
  private Long id;

  @Column(name = "chat_thread_id", nullable = false)
  private Long threadId;

  @Column(name = "sender_app_user_id", nullable = false)
  private Long senderAppUserId;

  @Column(nullable = false)
  private String body;

  @Column(name = "idempotency_key", nullable = false)
  private String idempotencyKey;

  @Column(name = "sent_at", insertable = false, updatable = false)
  private Instant sentAt;

  @Column(name = "read_by_counterparty_at")
  private Instant readByCounterpartyAt;

  public static ChatMessageEntity create(
      long threadId, long senderAppUserId, String body, String idempotencyKey) {
    var message = new ChatMessageEntity();
    message.threadId = threadId;
    message.senderAppUserId = senderAppUserId;
    message.body = body;
    message.idempotencyKey = idempotencyKey;
    return message;
  }
}
