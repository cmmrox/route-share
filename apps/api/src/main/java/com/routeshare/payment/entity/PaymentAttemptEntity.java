package com.routeshare.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One call to the payment provider, recorded <em>before</em> it is made.
 *
 * <p>A capture that times out has either happened or it has not, and the difference is a double
 * charge. Writing the attempt first — with a deterministic idempotency key the database keeps
 * unique — means a timeout leaves a row to reconcile against instead of a decision to guess at.
 *
 * <p>Stores the provider reference only. No PAN, no CVV, no token ever lands here.
 */
@Entity
@Table(name = "payment_attempt", schema = "payment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttemptEntity {
  public static final String STARTED = "STARTED";
  public static final String SUCCEEDED = "SUCCEEDED";
  public static final String FAILED = "FAILED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_attempt_id")
  private Long id;

  @Column(name = "payment_intent_id", nullable = false)
  private Long paymentIntentId;

  @Column(name = "booking_id")
  private Long bookingId;

  @Column(nullable = false)
  private String operation;

  @Column(name = "idempotency_key", nullable = false)
  private String idempotencyKey;

  @Column(name = "provider_reference")
  private String providerReference;

  private BigDecimal amount;

  @Column(nullable = false)
  private String currency = "LKR";

  @Column(nullable = false)
  private String status = STARTED;

  @Column(name = "failure_code")
  private String failureCode;

  @Column(name = "started_at", insertable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  public static PaymentAttemptEntity start(
      long paymentIntentId,
      Long bookingId,
      String operation,
      String idempotencyKey,
      String providerReference,
      BigDecimal amount,
      String currency) {
    var entity = new PaymentAttemptEntity();
    entity.paymentIntentId = paymentIntentId;
    entity.bookingId = bookingId;
    entity.operation = operation;
    entity.idempotencyKey = idempotencyKey;
    entity.providerReference = providerReference;
    entity.amount = amount;
    entity.currency = currency;
    entity.status = STARTED;
    return entity;
  }

  public void succeeded(Instant when) {
    this.status = SUCCEEDED;
    this.finishedAt = when;
  }

  public void failed(Instant when, String failureCode) {
    this.status = FAILED;
    this.finishedAt = when;
    this.failureCode = failureCode;
  }
}
