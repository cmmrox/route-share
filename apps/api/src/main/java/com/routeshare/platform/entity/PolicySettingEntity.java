package com.routeshare.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One runtime-tunable rule. Seeded by migration; edited by finance and ops, never by a deploy. */
@Entity
@Table(name = "policy_setting", schema = "platform")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicySettingEntity {
  @Id
  @Column(name = "policy_key")
  private String policyKey;

  @Column(nullable = false)
  private String value;

  @Column(name = "value_type", nullable = false)
  private String valueType;

  private String description;

  @Column(name = "updated_at", insertable = false)
  private Instant updatedAt;

  @Column(name = "updated_by_app_user_id")
  private Long updatedByAppUserId;
}
