package com.routeshare.passenger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "passenger_profile", schema = "passenger")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PassengerProfileEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "passenger_profile_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false, unique = true)
  private Long appUserId;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "photo_url")
  private String photoUrl;
}
