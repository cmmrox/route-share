package com.routeshare.payment.gateway;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Abstraction over the card payment provider (Cybersource in production). Card data never touches
 * RouteShare servers: the client tokenizes the PAN with Cybersource Microform/Flex and sends a
 * transient token, which we exchange for a stored instrument here. When card payments are disabled
 * the {@link com.routeshare.payment.gateway.impl.CashFallbackPaymentGateway} keeps the cash flow
 * working without contacting any provider.
 */
public interface PaymentGatewayPort {

  /** True when a real card provider is configured; false for the cash-only fallback. */
  boolean cardPaymentsEnabled();

  /** Authorize (pre-auth) a charge for a booking. */
  AuthorizationResult authorize(AuthorizeCommand command);

  /** Capture a previously authorized charge (typically the final fare after the trip). */
  void capture(String providerReference, BigDecimal amount, String currency);

  /** Void an authorization that was never captured (e.g. cancellation before trip). */
  void voidAuthorization(String providerReference);

  /** Refund a captured charge, fully or partially. */
  void refund(String providerReference, BigDecimal amount, String currency);

  /** Exchange a client transient token for a stored, reusable instrument. */
  TokenizationResult tokenizeCard(String transientToken);

  /** Verifies a provider webhook signature against the raw body. */
  boolean verifyWebhookSignature(String rawBody, Map<String, String> headers);

  record AuthorizeCommand(
      long bookingId, BigDecimal amount, String currency, String paymentToken) {}

  record AuthorizationResult(String providerReference, String status, boolean approved) {}

  record TokenizationResult(
      String token, String brand, String last4, Integer expMonth, Integer expYear) {}
}
