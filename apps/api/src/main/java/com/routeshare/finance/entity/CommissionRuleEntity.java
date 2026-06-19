package com.routeshare.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "commission_rule", schema = "finance")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommissionRuleEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "commission_rule_id")
  private Long id;

  @Column(nullable = false)
  private String scope = "GLOBAL";

  @Column(name = "scope_ref")
  private String scopeRef;

  @Column(nullable = false)
  private BigDecimal rate;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public static CommissionRuleEntity of(String scope, String scopeRef, BigDecimal rate) {
    var e = new CommissionRuleEntity();
    e.scope = scope == null || scope.isBlank() ? "GLOBAL" : scope;
    e.scopeRef = scopeRef;
    e.rate = rate;
    e.active = true;
    e.updatedAt = Instant.now();
    return e;
  }
}
