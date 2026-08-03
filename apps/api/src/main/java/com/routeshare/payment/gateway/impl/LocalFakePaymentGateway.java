package com.routeshare.payment.gateway.impl;

import com.routeshare.payment.gateway.PaymentGatewayPort;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Local-only card state machine for runtime QA when the Cybersource sandbox is unavailable.
 *
 * <p>This adapter never handles a PAN, never calls a provider and is opt-in. Production defaults
 * keep it absent; enabling it together with Cybersource is rejected by the bean condition. It
 * exists so authorize/capture/void/refund integration paths can be exercised honestly instead of
 * being silently skipped.
 */
@Component
@ConditionalOnExpression(
    "'${routeshare.payment.local-fake-enabled:false}' == 'true' &&"
        + " '${routeshare.cybersource.enabled:false}' == 'false'")
public class LocalFakePaymentGateway implements PaymentGatewayPort {
  private enum State {
    AUTHORIZED,
    CAPTURED,
    VOIDED,
    REFUNDED
  }

  private final Map<String, State> authorizations = new ConcurrentHashMap<>();

  @Override
  public String providerCode() {
    return "LOCAL_FAKE";
  }

  @Override
  public boolean cardPaymentsEnabled() {
    return true;
  }

  @Override
  public AuthorizationResult authorize(AuthorizeCommand command) {
    requireText(command.paymentToken(), "payment token");
    String reference = "local_qa_auth_" + UUID.randomUUID();
    authorizations.put(reference, State.AUTHORIZED);
    return new AuthorizationResult(reference, "REQUIRES_CAPTURE", true);
  }

  @Override
  public void capture(String providerReference, BigDecimal amount, String currency) {
    transition(providerReference, State.AUTHORIZED, State.CAPTURED);
  }

  @Override
  public void voidAuthorization(String providerReference) {
    transition(providerReference, State.AUTHORIZED, State.VOIDED);
  }

  @Override
  public void refund(String providerReference, BigDecimal amount, String currency) {
    transition(providerReference, State.CAPTURED, State.REFUNDED);
  }

  @Override
  public TokenizationResult tokenizeCard(String transientToken) {
    requireText(transientToken, "transient token");
    return new TokenizationResult("local_qa_token_" + UUID.randomUUID(), "VISA", "4242", 12, 2030);
  }

  @Override
  public boolean verifyWebhookSignature(String rawBody, Map<String, String> headers) {
    return false;
  }

  private void transition(String reference, State expected, State target) {
    authorizations.compute(
        requireText(reference, "provider reference"),
        (ignored, current) -> {
          if (current != expected) {
            throw new IllegalStateException(
                "Local QA payment cannot move from " + current + " to " + target);
          }
          return target;
        });
  }

  private static String requireText(String value, String label) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(label + " is required");
    }
    return value;
  }
}
