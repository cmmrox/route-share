package com.routeshare.routing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "route_occurrence", schema = "routing")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RouteOccurrenceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "route_occurrence_id")
  private Long id;

  @Column(name = "route_plan_id")
  private Long routePlanId;

  @Column(name = "scheduled_departure_at")
  private Instant scheduledDepartureAt;

  @Column(name = "available_seats")
  private Integer availableSeats;

  private String status;
}
