package com.routeshare.location.entity;

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
import lombok.Setter;

@Entity
@Table(name = "location_sample", schema = "location")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocationSampleEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "location_sample_id")
  private Long id;

  @Column(name = "trip_id")
  private Long tripId;

  @Column(name = "driver_profile_id")
  private Long driverProfileId;

  @Column(name = "accuracy_m")
  private BigDecimal accuracyMeters;

  @Column(name = "speed_mps")
  private BigDecimal speedMps;

  @Column(name = "bearing_degrees")
  private BigDecimal bearingDegrees;

  @Column(name = "device_recorded_at")
  private Instant deviceRecordedAt;
}
