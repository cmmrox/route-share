package com.routeshare.payment.gateway.impl;

import com.routeshare.payment.gateway.PaymentGatewayPort;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Default gateway used when Cybersource is disabled. It supports the cash-collection flow (a local
 * payment-intent reference with no external authorization) and fails closed on any card-only
 * operation so the system never pretends a real card was charged.
 */
@Component
@ConditionalOnProperty(
    prefix = "routeshare.cybersource",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class CashFallbackPaymentGateway implements PaymentGatewayPort {

  @Override
  public boolean cardPaymentsEnabled() {
    return false;
  }

  @Override
  public AuthorizationResult authorize(AuthorizeCommand command) {
    // No external provider: produce a local reference so the intent/ledger flow works for cash.
    return new AuthorizationResult("local_" + UUID.randomUUID(), "REQUIRES_CAPTURE", true);
  }

  @Override
  public void capture(String providerReference, BigDecimal amount, String currency) {
    // Local capture is a no-op; settlement is tracked by the fare ledger.
  }

  @Override
  public void voidAuthorization(String providerReference) {
    // no-op for local references
  }

  @Override
  public void refund(String providerReference, BigDecimal amount, String currency) {
    // no-op for local references; refunds are reflected in the ledger
  }

  @Override
  public TokenizationResult tokenizeCard(String transientToken) {
    throw new ResponseStatusException(
        HttpStatus.PRECONDITION_FAILED,
        "Card payments are not enabled. Set routeshare.cybersource.enabled=true to add cards.");
  }

  @Override
  public boolean verifyWebhookSignature(String rawBody, Map<String, String> headers) {
    return false;
  }
}
