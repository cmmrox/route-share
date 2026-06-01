package com.routeshare.trip.entity;

import com.routeshare.trip.domain.PassengerTripStatus;
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
@Table(name = "passenger_trip_state", schema = "trip")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PassengerTripStateEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "passenger_trip_state_id")
  private Long id;

  @Column(name = "trip_id")
  private Long tripId;

  @Column(name = "booking_id")
  private Long bookingId;

  @Column(name = "route_occurrence_id")
  private Long routeOccurrenceId;

  @Column(name = "passenger_app_user_id")
  private Long passengerAppUserId;

  @Enumerated(EnumType.STRING)
  private PassengerTripStatus status;

  @Column(name = "boarded_at")
  private Instant boardedAt;

  @Column(name = "no_show_at")
  private Instant noShowAt;

  @Column(name = "dropped_off_at")
  private Instant droppedOffAt;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;
}
