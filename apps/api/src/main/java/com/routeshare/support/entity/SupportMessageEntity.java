package com.routeshare.support.entity;

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
@Table(name = "support_message", schema = "support")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportMessageEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "support_message_id")
  private Long id;

  @Column(name = "support_ticket_id", nullable = false)
  private Long supportTicketId;

  @Column(name = "sender_app_user_id", nullable = false)
  private Long senderAppUserId;

  @Column(name = "sender_role", nullable = false)
  private String senderRole;

  @Column(nullable = false)
  private String body;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static SupportMessageEntity of(
      long ticketId, long senderAppUserId, String senderRole, String body) {
    var e = new SupportMessageEntity();
    e.supportTicketId = ticketId;
    e.senderAppUserId = senderAppUserId;
    e.senderRole = senderRole;
    e.body = body;
    return e;
  }
}
