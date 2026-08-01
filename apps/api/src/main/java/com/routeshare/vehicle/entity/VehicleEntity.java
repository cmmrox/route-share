package com.routeshare.vehicle.entity;

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
@Table(name = "vehicle", schema = "vehicle")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class VehicleEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "vehicle_id")
  private Long id;

  @Column(name = "driver_profile_id", nullable = false)
  private Long driverProfileId;

  private String make;
  private String model;

  @Column(name = "manufacture_year")
  private Integer manufactureYear;

  private String color;

  @Column(name = "registration_number")
  private String registrationNumber;

  @Column(name = "seat_count")
  private Integer seatCount;

  @Column(insertable = false)
  private String status;

  @Column(name = "class_key", nullable = false)
  private String classKey;
}
