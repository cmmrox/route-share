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
}
