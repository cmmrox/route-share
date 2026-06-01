package com.routeshare.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fare_ledger_entry", schema = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FareLedgerEntryEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "fare_ledger_entry_id")
  private Long id;

  @Column(name = "booking_id")
  private Long bookingId;

  @Column(name = "entry_type")
  private String entryType;

  private BigDecimal amount;
  private String currency;

  @Column(name = "created_at")
  private Instant createdAt;
}
