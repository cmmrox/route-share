package com.routeshare.rewards.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rewards_ledger", schema = "rewards")
@Getter
@NoArgsConstructor
public class RewardsLedgerEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "rewards_ledger_id")
  private Long id;

  @Column(name = "app_user_id")
  private Long appUserId;

  private String kind;
  private BigDecimal amount;
  private String label;
  private String sublabel;

  @Column(name = "occurred_at")
  private Instant occurredAt;

  @Column(name = "idempotency_key")
  private String idempotencyKey;
}
