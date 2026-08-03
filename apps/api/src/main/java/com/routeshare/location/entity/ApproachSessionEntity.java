package com.routeshare.location.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "approach_session", schema = "location")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApproachSessionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "approach_session_id")
  private Long id;

  @Column(name = "trip_id")
  private Long tripId;

  @Column(name = "booking_id")
  private Long bookingId;

  @Column(name = "opened_at")
  private Instant openedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Column(name = "rider_position_at")
  private Instant riderPositionAt;
}
