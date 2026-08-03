package com.routeshare.payment.gateway.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.routeshare.payment.gateway.PaymentGatewayPort;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LocalFakePaymentGatewayTest {
  private final LocalFakePaymentGateway gateway = new LocalFakePaymentGateway();

  @Test
  void exercisesTheLocalCardLifecycleWithoutPretendingToBeAProvider() {
    var card = gateway.tokenizeCard("qa-transient-token");
    var authorization =
        gateway.authorize(
            new PaymentGatewayPort.AuthorizeCommand(
                42L, new BigDecimal("267.00"), "LKR", card.token()));

    assertThat(gateway.cardPaymentsEnabled()).isTrue();
    assertThat(gateway.providerCode()).isEqualTo("LOCAL_FAKE");
    assertThat(card.last4()).isEqualTo("4242");
    gateway.capture(authorization.providerReference(), new BigDecimal("267.00"), "LKR");
    gateway.refund(authorization.providerReference(), new BigDecimal("20.00"), "LKR");
  }

  @Test
  void refusesImpossibleProviderTransitions() {
    var authorization =
        gateway.authorize(
            new PaymentGatewayPort.AuthorizeCommand(
                42L, new BigDecimal("267.00"), "LKR", "qa-token"));
    gateway.voidAuthorization(authorization.providerReference());

    assertThatThrownBy(
            () ->
                gateway.capture(authorization.providerReference(), new BigDecimal("267.00"), "LKR"))
        .isInstanceOf(IllegalStateException.class);
  }
}
