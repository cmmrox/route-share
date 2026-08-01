package com.routeshare.platform.entity;

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

/** Who changed which rule, from what to what. A price rule without this is unauditable. */
@Entity
@Table(name = "policy_setting_history", schema = "platform")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicySettingHistoryEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "policy_setting_history_id")
  private Long id;

  @Column(name = "policy_key", nullable = false)
  private String policyKey;

  @Column(name = "old_value")
  private String oldValue;

  @Column(name = "new_value", nullable = false)
  private String newValue;

  @Column(name = "changed_at", insertable = false, updatable = false)
  private Instant changedAt;

  @Column(name = "changed_by_app_user_id")
  private Long changedByAppUserId;

  public static PolicySettingHistoryEntity of(
      String policyKey, String oldValue, String newValue, Long changedByAppUserId) {
    var entity = new PolicySettingHistoryEntity();
    entity.policyKey = policyKey;
    entity.oldValue = oldValue;
    entity.newValue = newValue;
    entity.changedByAppUserId = changedByAppUserId;
    return entity;
  }
}
