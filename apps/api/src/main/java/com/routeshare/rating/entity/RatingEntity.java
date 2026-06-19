package com.routeshare.rating.entity;

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
@Table(name = "rating", schema = "rating")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RatingEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "rating_id")
  private Long id;

  @Column(name = "booking_id", nullable = false)
  private Long bookingId;

  @Column(name = "rater_app_user_id", nullable = false)
  private Long raterAppUserId;

  @Column(name = "ratee_app_user_id")
  private Long rateeAppUserId;

  @Column(name = "rater_role", nullable = false)
  private String raterRole;

  @Column(nullable = false)
  private int stars;

  private String comment;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public static RatingEntity create(
      long bookingId,
      long raterAppUserId,
      Long rateeAppUserId,
      String raterRole,
      int stars,
      String comment) {
    var e = new RatingEntity();
    e.bookingId = bookingId;
    e.raterAppUserId = raterAppUserId;
    e.rateeAppUserId = rateeAppUserId;
    e.raterRole = raterRole;
    e.stars = stars;
    e.comment = comment;
    return e;
  }
}
