package com.routeshare.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "booking", schema = "booking")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "booking_id")
  private Long id;

  @Column(name = "route_plan_id")
  private Long routePlanId;

  @Column(name = "route_occurrence_id")
  private Long routeOccurrenceId;

  @Column(name = "passenger_app_user_id")
  private Long passengerAppUserId;

  private Integer seats;
  private String status;

  @Column(name = "fare_estimate")
  private BigDecimal fareEstimate;

  @Column(name = "pickup_route_fraction")
  private BigDecimal pickupRouteFraction;

  @Column(name = "dropoff_route_fraction")
  private BigDecimal dropoffRouteFraction;
}
