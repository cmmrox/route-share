package com.routeshare.booking.entity;

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
@Table(name = "booking_status_history", schema = "booking")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingStatusHistoryEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "booking_status_history_id")
  private Long id;

  @Column(name = "booking_id")
  private Long bookingId;

  @Column(name = "from_status")
  private String fromStatus;

  @Column(name = "to_status")
  private String toStatus;

  @Column(name = "changed_by_app_user_id")
  private Long changedByAppUserId;

  private String reason;

  @Column(name = "changed_at")
  private Instant changedAt;
}
