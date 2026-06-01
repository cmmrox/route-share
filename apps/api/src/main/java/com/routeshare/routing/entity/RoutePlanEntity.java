package com.routeshare.routing.entity;

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
@Table(name = "route_plan", schema = "routing")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutePlanEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "route_plan_id")
  private Long id;

  @Column(name = "driver_profile_id")
  private Long driverProfileId;

  @Column(name = "vehicle_id")
  private Long vehicleId;

  @Column(name = "origin_label")
  private String originLabel;

  @Column(name = "destination_label")
  private String destinationLabel;

  @Column(name = "route_length_m")
  private BigDecimal routeLengthMeters;

  @Column(name = "departure_time")
  private Instant departureTime;

  @Column(name = "available_seats")
  private Integer availableSeats;

  private String status;
}
