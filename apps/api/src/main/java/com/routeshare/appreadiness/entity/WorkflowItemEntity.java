package com.routeshare.appreadiness.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "workflow_item", schema = "app_backend")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowItemEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "workflow_item_id")
  private Long id;

  @Column(name = "item_type", nullable = false)
  private String itemType;

  @Column(name = "owner_role")
  private String ownerRole;

  @Column(name = "owner_app_user_id")
  private Long ownerAppUserId;

  @Column(name = "target_type")
  private String targetType;

  @Column(name = "target_id")
  private String targetId;

  @Column(nullable = false)
  private String status = "ACTIVE";

  private String title;

  @Column(name = "payload_json")
  private String payloadJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public static WorkflowItemEntity create(
      String itemType,
      String ownerRole,
      Long ownerAppUserId,
      String targetType,
      String targetId,
      String status,
      String title,
      String payloadJson) {
    var entity = new WorkflowItemEntity();
    entity.itemType = itemType;
    entity.ownerRole = ownerRole;
    entity.ownerAppUserId = ownerAppUserId;
    entity.targetType = targetType;
    entity.targetId = targetId;
    entity.status = status;
    entity.title = title;
    entity.payloadJson = payloadJson;
    return entity;
  }

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }
}
