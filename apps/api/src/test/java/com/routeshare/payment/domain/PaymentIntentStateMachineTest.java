package com.routeshare.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.routeshare.payment.entity.PaymentIntentEntity;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * The states someone's money can be in, and the moves between them.
 *
 * <p>The transitions that must be impossible are the point: capturing what was never authorised,
 * capturing twice, and voiding what has already been taken are each a different way of getting a
 * real person's money wrong.
 */
class PaymentIntentStateMachineTest {
  private static final Instant NOW = Instant.parse("2026-08-02T09:41:00Z");

  private static PaymentIntentEntity pending() {
    return PaymentIntentEntity.pending(42L, new BigDecimal("267.00"), "LKR", 7L);
  }

  private static PaymentIntentEntity authorized() {
    var intent = pending();
    intent.authorize("auth_ref", NOW);
    return intent;
  }

  @Test
  void aBookingStartsPendingNotAuthorized() {
    // "We have not asked your bank yet" and "your bank is holding this" are different facts, and
    // the booking screens state each of them differently.
    assertThat(PaymentIntentStatus.of(pending().getStatus()))
        .isEqualTo(PaymentIntentStatus.PENDING);
  }

  @Test
  void authorizingRecordsTheReferenceAndTheMoment() {
    var intent = authorized();

    assertThat(intent.getStatus()).isEqualTo("AUTHORIZED");
    assertThat(intent.getProviderReference()).isEqualTo("auth_ref");
    assertThat(intent.getAuthorizedAt()).isEqualTo(NOW);
    assertThat(intent.getCapturedAt()).isNull();
  }

  @Test
  void capturingRecordsTheMomentP12ShowsThePassenger() {
    var intent = authorized();

    intent.capture(NOW);

    assertThat(intent.getStatus()).isEqualTo("CAPTURED");
    assertThat(intent.getCapturedAt()).isEqualTo(NOW);
  }

  @Test
  void moneyCannotBeTakenBeforeItIsHeld() {
    assertThatThrownBy(() -> pending().capture(NOW))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("PENDING");
  }

  @Test
  void moneyCannotBeTakenTwice() {
    var intent = authorized();
    intent.capture(NOW);

    // A retried "start trip" tap reaches here; the second capture must be refused outright.
    assertThatThrownBy(() -> intent.capture(NOW)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void aCapturedPaymentCannotBeVoided() {
    var intent = authorized();
    intent.capture(NOW);

    // Releasing is not the remedy once money has moved — refunding is, and that is a different
    // decision with a different owner.
    assertThatThrownBy(() -> intent.voidAuthorization(NOW))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void voidingReleasesTheHoldAndRecordsWhen() {
    var intent = authorized();

    intent.voidAuthorization(NOW);

    assertThat(intent.getStatus()).isEqualTo("VOIDED");
    assertThat(intent.getVoidedAt()).isEqualTo(NOW);
    assertThat(intent.getCapturedAt()).isNull();
  }

  @Test
  void aVoidedHoldIsFinal() {
    var intent = authorized();
    intent.voidAuthorization(NOW);

    assertThatThrownBy(() -> intent.capture(NOW)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void theLegacyStatusStillTransitionsSoOldRowsAreNotStranded() {
    assertThat(PaymentIntentStatus.REQUIRES_CAPTURE.canTransitionTo(PaymentIntentStatus.CAPTURED))
        .isTrue();
    assertThat(PaymentIntentStatus.REQUIRES_CAPTURE.isAuthorizedNotCaptured()).isTrue();
  }

  @Test
  void aFailureCanBeRecordedFromAnyStateTheGatewayWasCalledIn() {
    var intent = authorized();

    intent.fail("GATEWAY_ERROR", NOW);

    // Refusing to record a failure would lose the only evidence of what the provider said.
    assertThat(intent.getStatus()).isEqualTo("FAILED");
    assertThat(intent.getFailureCode()).isEqualTo("GATEWAY_ERROR");
  }

  @Test
  void aFailedAuthorizationCanBeRetried() {
    assertThat(PaymentIntentStatus.FAILED.canTransitionTo(PaymentIntentStatus.AUTHORIZED)).isTrue();
  }

  @Test
  void settledAndHeldAreDistinguishable() {
    assertThat(PaymentIntentStatus.CAPTURED.isSettled()).isTrue();
    assertThat(PaymentIntentStatus.REFUNDED.isSettled()).isTrue();
    assertThat(PaymentIntentStatus.AUTHORIZED.isSettled()).isFalse();
    assertThat(PaymentIntentStatus.AUTHORIZED.isAuthorizedNotCaptured()).isTrue();
  }
}
