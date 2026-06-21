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
@Table(name = "trip_share", schema = "booking")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripShareEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "trip_share_id")
  private Long id;

  @Column(name = "booking_id", nullable = false)
  private Long bookingId;

  @Column(name = "passenger_app_user_id", nullable = false)
  private Long passengerAppUserId;

  @Column(nullable = false)
  private String token;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean revoked;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static TripShareEntity create(
      long bookingId, long passengerAppUserId, String token, Instant expiresAt) {
    var entity = new TripShareEntity();
    entity.bookingId = bookingId;
    entity.passengerAppUserId = passengerAppUserId;
    entity.token = token;
    entity.expiresAt = expiresAt;
    entity.revoked = false;
    return entity;
  }
}
