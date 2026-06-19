package com.routeshare.admin.entity;

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
@Table(name = "audit_action", schema = "audit")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditActionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "audit_action_id")
  private Long id;

  @Column(name = "actor_app_user_id")
  private Long actorAppUserId;

  @Column(name = "actor_role")
  private String actorRole;

  @Column(nullable = false)
  private String action;

  @Column(name = "target_type")
  private String targetType;

  @Column(name = "target_id")
  private String targetId;

  @Column(name = "detail_json")
  private String detailJson;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static AuditActionEntity of(
      Long actorAppUserId,
      String actorRole,
      String action,
      String targetType,
      String targetId,
      String detailJson) {
    var e = new AuditActionEntity();
    e.actorAppUserId = actorAppUserId;
    e.actorRole = actorRole;
    e.action = action;
    e.targetType = targetType;
    e.targetId = targetId;
    e.detailJson = detailJson;
    return e;
  }
}
