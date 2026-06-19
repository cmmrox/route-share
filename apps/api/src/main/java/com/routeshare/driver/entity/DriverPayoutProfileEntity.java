package com.routeshare.driver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "driver_payout_profile", schema = "driver")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DriverPayoutProfileEntity {
  public static final String PENDING = "PENDING_VERIFICATION";

  @Id
  @Column(name = "driver_profile_id")
  private Long driverProfileId;

  @Column(nullable = false)
  private String method = "BANK_TRANSFER";

  @Column(name = "bank_name")
  private String bankName;

  private String branch;

  @Column(name = "account_name")
  private String accountName;

  @Column(name = "account_number")
  private String accountNumber;

  @Column(name = "wallet_provider")
  private String walletProvider;

  @Column(name = "wallet_number")
  private String walletNumber;

  @Column(nullable = false)
  private String status = PENDING;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public static DriverPayoutProfileEntity blank(long driverProfileId) {
    var e = new DriverPayoutProfileEntity();
    e.driverProfileId = driverProfileId;
    e.status = PENDING;
    e.updatedAt = Instant.now();
    return e;
  }
}
