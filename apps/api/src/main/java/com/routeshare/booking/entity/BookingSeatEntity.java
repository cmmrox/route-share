package com.routeshare.booking.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A live claim on one named seat.
 *
 * <p>The partial unique index behind this table — one unreleased hold per seat — is what makes the
 * race safe. Releasing sets a timestamp rather than deleting the row, so "who held slot 3 before it
 * was resold" stays answerable.
 */
@Entity
@Table(name = "booking_seat", schema = "booking")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingSeatEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "booking_seat_id")
  private Long id;

  @Column(name = "booking_id", nullable = false)
  private Long bookingId;

  @Column(name = "route_occurrence_seat_id", nullable = false)
  private Long routeOccurrenceSeatId;

  @Column(name = "held_at", insertable = false, updatable = false)
  private Instant heldAt;

  @Column(name = "released_at")
  private Instant releasedAt;

  public static BookingSeatEntity hold(long bookingId, long routeOccurrenceSeatId) {
    var entity = new BookingSeatEntity();
    entity.bookingId = bookingId;
    entity.routeOccurrenceSeatId = routeOccurrenceSeatId;
    return entity;
  }

  public boolean isLive() {
    return releasedAt == null;
  }

  /** Idempotent: a hold released twice keeps the instant it was first given up. */
  public void release(Instant at) {
    if (releasedAt == null) {
      this.releasedAt = at;
    }
  }
}
