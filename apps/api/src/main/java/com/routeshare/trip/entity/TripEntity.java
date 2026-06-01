package com.routeshare.trip.entity;

import com.routeshare.trip.domain.TripStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trip", schema = "trip")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "trip_id")
  private Long id;

  @Column(name = "route_plan_id")
  private Long routePlanId;

  @Enumerated(EnumType.STRING)
  private TripStatus status;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;
}
