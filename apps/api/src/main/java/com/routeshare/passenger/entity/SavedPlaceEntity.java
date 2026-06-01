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
@Table(name = "saved_place", schema = "passenger")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedPlaceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "saved_place_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  private String label;
  private String address;
}
