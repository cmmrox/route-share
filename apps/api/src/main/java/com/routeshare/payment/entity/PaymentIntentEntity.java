package com.routeshare.payment.entity;

import com.routeshare.payment.domain.PaymentIntentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The state of one booking's money.
 *
 * <p>Transitions go through the methods below rather than {@code setStatus}, so the timestamp that
 * proves when something happened cannot be forgotten: P12 shows the passenger the exact minute
 * their card was charged, and a captured row with no {@code capturedAt} cannot answer that.
 */
@Entity
@Table(name = "payment_intent", schema = "payment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentIntentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_intent_id")
  private Long id;

  @Column(name = "booking_id")
  private Long bookingId;

  @Column(insertable = false)
  private String provider;

  @Column(name = "provider_reference")
  private String providerReference;

  private BigDecimal amount;
  private String currency;

  @Column(nullable = false)
  private String status;

  @Column(name = "authorized_at")
  private Instant authorizedAt;

  @Column(name = "captured_at")
  private Instant capturedAt;

  @Column(name = "voided_at")
  private Instant voidedAt;

  @Column(name = "failure_code")
  private String failureCode;

  @Column(name = "failure_message")
  private String failureMessage;

  @Column(name = "payment_method_id")
  private Long paymentMethodId;

  @Column(name = "attempt_count", insertable = false)
  private Integer attemptCount = 0;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  /**
   * A hold that has not been asked for yet. The provider reference is a local placeholder until the
   * gateway answers — the column is NOT NULL and unique, and a booking must not fail because the
   * bank was slow.
   */
  public static PaymentIntentEntity pending(
      long bookingId, BigDecimal amount, String currency, Long paymentMethodId) {
    var entity = new PaymentIntentEntity();
    entity.bookingId = bookingId;
    entity.amount = amount;
    entity.currency = currency;
    entity.paymentMethodId = paymentMethodId;
    entity.providerReference = "local_" + java.util.UUID.randomUUID();
    entity.status = PaymentIntentStatus.PENDING.name();
    entity.attemptCount = 0;
    return entity;
  }

  public void authorize(String providerReference, Instant when) {
    requireTransition(PaymentIntentStatus.AUTHORIZED);
    this.providerReference = providerReference;
    this.status = PaymentIntentStatus.AUTHORIZED.name();
    this.authorizedAt = when;
    this.failureCode = null;
    this.failureMessage = null;
  }

  public void capture(Instant when) {
    requireTransition(PaymentIntentStatus.CAPTURED);
    this.status = PaymentIntentStatus.CAPTURED.name();
    this.capturedAt = when;
  }

  public void voidAuthorization(Instant when) {
    requireTransition(PaymentIntentStatus.VOIDED);
    this.status = PaymentIntentStatus.VOIDED.name();
    this.voidedAt = when;
  }

  /**
   * A refusal from the provider. Deliberately not guarded: a failure can arrive from any state the
   * gateway was called in, and refusing to record it would lose the only evidence of what happened.
   */
  public void fail(String failureCode, Instant when) {
    this.status = PaymentIntentStatus.FAILED.name();
    this.failureCode = failureCode;
  }

  public void recordAttempt() {
    this.attemptCount = (this.attemptCount == null ? 0 : this.attemptCount) + 1;
  }

  private void requireTransition(PaymentIntentStatus target) {
    PaymentIntentStatus from = PaymentIntentStatus.of(status);
    if (!from.canTransitionTo(target)) {
      throw new IllegalStateException(
          "Payment cannot go from " + from + " to " + target + " for booking " + bookingId);
    }
  }
}
