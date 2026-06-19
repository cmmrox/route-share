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
@Table(name = "support_ticket", schema = "support")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportTicketEntity {
  public static final String OPEN = "OPEN";
  public static final String PENDING = "PENDING";
  public static final String RESOLVED = "RESOLVED";
  public static final String CLOSED = "CLOSED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "support_ticket_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Column(name = "owner_role", nullable = false)
  private String ownerRole;

  @Column(nullable = false)
  private String subject;

  private String category;

  @Column(nullable = false)
  private String status = OPEN;

  @Column(nullable = false)
  private String priority = "NORMAL";

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public static SupportTicketEntity open(
      long appUserId, String ownerRole, String subject, String category, String priority) {
    var e = new SupportTicketEntity();
    e.appUserId = appUserId;
    e.ownerRole = ownerRole;
    e.subject = subject;
    e.category = category;
    e.priority = priority == null || priority.isBlank() ? "NORMAL" : priority;
    e.status = OPEN;
    e.updatedAt = Instant.now();
    return e;
  }
}
