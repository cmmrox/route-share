package com.routeshare.penalty.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A fee that could not be taken, riding along to the next booking (P25, P09d).
 *
 * <p>A cash passenger has no card to charge, and refusing her next booking over an unpaid fee would
 * turn a LKR 49 no-show into a lost customer. P25 shows dues as a line on the next checkout, not a
 * gate on making one.
 */
@Entity
@Table(name = "passenger_due", schema = "penalty")
@Getter
@NoArgsConstructor
public class PassengerDueEntity {
  public static final String STATUS_OUTSTANDING = "OUTSTANDING";
  public static final String STATUS_SETTLED = "SETTLED";
  public static final String STATUS_WAIVED = "WAIVED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "passenger_due_id")
  private Long id;

  @Column(name = "app_user_id", nullable = false)
  private Long appUserId;

  @Column(name = "penalty_id", nullable = false)
  private Long penaltyId;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private String reason;

  @Column(name = "origin_booking_id")
  private Long originBookingId;

  @Column(nullable = false)
  private String status;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "settled_at")
  private Instant settledAt;

  @Column(name = "settled_booking_id")
  private Long settledBookingId;

  public static PassengerDueEntity outstanding(
      long appUserId, long penaltyId, BigDecimal amount, String reason, Long originBookingId) {
    var entity = new PassengerDueEntity();
    entity.appUserId = appUserId;
    entity.penaltyId = penaltyId;
    entity.amount = amount;
    entity.reason = reason;
    entity.originBookingId = originBookingId;
    entity.status = STATUS_OUTSTANDING;
    return entity;
  }

  public boolean isOutstanding() {
    return STATUS_OUTSTANDING.equals(status);
  }

  /**
   * Attaches the due to the checkout that is carrying it, without settling it. P09d shows the line
   * at booking; the money only moves when that booking's card is actually captured, so a booking
   * that never starts must leave the fee outstanding rather than quietly clearing it.
   */
  public void carriedBy(long bookingId) {
    if (isOutstanding()) {
      this.settledBookingId = bookingId;
    }
  }

  /** The carrying booking fell through. The fee rides on to the next one. */
  public void release() {
    if (isOutstanding()) {
      this.settledBookingId = null;
    }
  }

  /**
   * Settled by the booking that carried it — recorded so P25's history can say which trip finally
   * paid off which fee.
   */
  public void settle(long settledBookingId, Instant at) {
    if (!isOutstanding()) {
      return;
    }
    this.status = STATUS_SETTLED;
    this.settledBookingId = settledBookingId;
    this.settledAt = at;
  }

  public void waive(Instant at) {
    if (!isOutstanding()) {
      return;
    }
    this.status = STATUS_WAIVED;
    this.settledAt = at;
  }
}
