package com.routeshare.driver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "driver_profile", schema = "driver")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DriverProfileEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "driver_profile_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false, unique = true)
  private Long appUserId;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "verification_status", nullable = false)
  private String verificationStatus;

  /**
   * Written by KYC review from the NIC, never self-declared. Its only use is D35's women-only set
   * gate — "Your NIC verifies you as female" — so it is null until a reviewer has read the card.
   */
  @Column(name = "gender")
  private String gender;

  public DriverProfileEntity(
      Long id, Long appUserId, String displayName, String verificationStatus) {
    this(id, appUserId, displayName, verificationStatus, null);
  }
}
