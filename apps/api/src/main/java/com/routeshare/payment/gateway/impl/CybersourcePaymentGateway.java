package com.routeshare.payment.gateway.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.payment.gateway.PaymentGatewayPort;
import com.routeshare.payment.gateway.config.CybersourceProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Real Cybersource REST adapter. Active only when {@code routeshare.cybersource.enabled=true}. Card
 * data is never received here — the client tokenizes via Cybersource Microform and sends a
 * transient token which we attach to the authorization. Each request is signed with {@link
 * CybersourceSigner}.
 */
@Component
@ConditionalOnProperty(prefix = "routeshare.cybersource", name = "enabled", havingValue = "true")
public class CybersourcePaymentGateway implements PaymentGatewayPort {
  private static final Logger log = LoggerFactory.getLogger(CybersourcePaymentGateway.class);

  private final CybersourceProperties props;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final CybersourceSigner signer;

  public CybersourcePaymentGateway(CybersourceProperties props, ObjectMapper objectMapper) {
    if (!props.ready()) {
      throw new IllegalStateException(
          "Cybersource is enabled but merchantId/keyId/sharedSecret are missing");
    }
    this.props = props;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    this.signer = new CybersourceSigner(props.keyId(), props.sharedSecret());
  }

  @Override
  public boolean cardPaymentsEnabled() {
    return true;
  }

  @Override
  public AuthorizationResult authorize(AuthorizeCommand command) {
    String body =
        "{\"clientReferenceInformation\":{\"code\":\"booking-"
            + command.bookingId()
            + "\"},\"processingInformation\":{\"capture\":false},"
            + "\"paymentInformation\":{\"paymentInstrument\":{\"id\":\""
            + escape(command.paymentToken())
            + "\"}},\"orderInformation\":{\"amountDetails\":{\"totalAmount\":\""
            + money(command.amount())
            + "\",\"currency\":\""
            + command.currency()
            + "\"}}}";
    JsonNode res = post("/pts/v2/payments", body);
    String id = res.path("id").asText(null);
    String status = res.path("status").asText("");
    boolean approved = "AUTHORIZED".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status);
    if (id == null || !approved) {
      throw new ResponseStatusException(
          HttpStatus.PAYMENT_REQUIRED, "Card authorization was declined (" + status + ")");
    }
    return new AuthorizationResult(id, "REQUIRES_CAPTURE", true);
  }

  @Override
  public void capture(String providerReference, BigDecimal amount, String currency) {
    String body =
        "{\"orderInformation\":{\"amountDetails\":{\"totalAmount\":\""
            + money(amount)
            + "\",\"currency\":\""
            + currency
            + "\"}}}";
    post("/pts/v2/payments/" + providerReference + "/captures", body);
  }

  @Override
  public void voidAuthorization(String providerReference) {
    post("/pts/v2/payments/" + providerReference + "/voids", "{}");
  }

  @Override
  public void refund(String providerReference, BigDecimal amount, String currency) {
    String body =
        "{\"orderInformation\":{\"amountDetails\":{\"totalAmount\":\""
            + money(amount)
            + "\",\"currency\":\""
            + currency
            + "\"}}}";
    post("/pts/v2/payments/" + providerReference + "/refunds", body);
  }

  @Override
  public TokenizationResult tokenizeCard(String transientToken) {
    String body =
        "{\"processingInformation\":{},\"tokenInformation\":{\"transientTokenJwt\":\""
            + escape(transientToken)
            + "\"}}";
    JsonNode res = post("/tms/v1/paymentinstruments", body);
    JsonNode card = res.path("card");
    return new TokenizationResult(
        res.path("id").asText(null),
        card.path("type").asText("CARD"),
        res.path("_embedded").path("instrumentIdentifier").path("card").path("number").asText(""),
        card.path("expirationMonth").isMissingNode() ? null : card.path("expirationMonth").asInt(),
        card.path("expirationYear").isMissingNode() ? null : card.path("expirationYear").asInt());
  }

  @Override
  public boolean verifyWebhookSignature(String rawBody, Map<String, String> headers) {
    String provided = headers.getOrDefault("v-c-signature", headers.get("V-C-Signature"));
    if (provided == null || props.webhookSecret() == null || props.webhookSecret().isBlank()) {
      return false;
    }
    var verifier = new CybersourceSigner(props.keyId(), props.webhookSecret());
    String expected = verifier.hmacBase64(rawBody);
    return MessageDigestEquals.constantTimeEquals(expected, provided);
  }

  private JsonNode post(String resource, String body) {
    try {
      String host = props.host();
      String date =
          DateTimeFormatter.RFC_1123_DATE_TIME.format(
              ZonedDateTime.now(java.time.ZoneOffset.UTC)
                  .withZoneSameInstant(java.time.ZoneId.of("GMT")));
      String digest = CybersourceSigner.digest(body);
      String signature =
          signer.signatureHeader("post", resource, host, date, props.merchantId(), digest);
      HttpRequest request =
          HttpRequest.newBuilder(URI.create("https://" + host + resource))
              .timeout(Duration.ofSeconds(20))
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .header("host", host)
              .header("date", date)
              .header("digest", digest)
              .header("v-c-merchant-id", props.merchantId())
              .header("signature", signature)
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("cybersource_error status={} resource={}", response.statusCode(), resource);
        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY, "Payment provider rejected the request");
      }
      return objectMapper.readTree(response.body());
    } catch (java.io.IOException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Payment provider is unavailable. Retry later.");
    }
  }

  private static String money(BigDecimal amount) {
    return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private static String escape(String s) {
    return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  /** Constant-time comparison to avoid timing leaks on webhook signature checks. */
  static final class MessageDigestEquals {
    static boolean constantTimeEquals(String a, String b) {
      return java.security.MessageDigest.isEqual(
          a.getBytes(java.nio.charset.StandardCharsets.UTF_8),
          b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private MessageDigestEquals() {}
  }
}
