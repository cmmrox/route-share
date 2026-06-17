package com.routeshare.payment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.payment.entity.PaymentWebhookEventEntity;
import com.routeshare.payment.gateway.PaymentGatewayPort;
import com.routeshare.payment.repository.PaymentIntentRepository;
import com.routeshare.payment.repository.PaymentWebhookEventRepository;
import com.routeshare.payment.service.PaymentWebhookService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {
  private static final Logger log = LoggerFactory.getLogger(PaymentWebhookServiceImpl.class);
  private static final String PROVIDER = "CYBERSOURCE";

  private final PaymentGatewayPort gateway;
  private final PaymentWebhookEventRepository webhookEvents;
  private final PaymentIntentRepository paymentIntents;
  private final DomainEventPublisher events;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public void handleCybersource(String rawBody, Map<String, String> headers) {
    if (rawBody == null || !gateway.verifyWebhookSignature(rawBody, lower(headers))) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
    }
    JsonNode root = parse(rawBody);
    String eventId = firstNonBlank(root.path("id").asText(null), root.path("eventId").asText(null));
    String eventType =
        firstNonBlank(root.path("eventType").asText(null), root.path("type").asText("unknown"));
    if (eventId == null) {
      log.warn("cybersource_webhook_missing_event_id type={}", eventType);
      return;
    }
    if (webhookEvents.existsByProviderAndEventId(PROVIDER, eventId)) {
      return; // already processed
    }
    webhookEvents.save(PaymentWebhookEventEntity.of(PROVIDER, eventId, eventType));

    String providerReference =
        firstNonBlank(
            root.path("data").path("paymentId").asText(null),
            root.path("paymentId").asText(null),
            root.path("data").path("id").asText(null));
    String mapped = mapStatus(eventType, root.path("data").path("status").asText(null));
    if (providerReference != null && mapped != null) {
      paymentIntents.updateStatusByProviderReference(providerReference, mapped);
    }
    events.publish(
        DomainEvent.of(
            "payment.webhook.received",
            "payment_intent",
            providerReference == null ? eventId : providerReference,
            "{\"eventType\":\"" + eventType + "\"}"));
  }

  private static String mapStatus(String eventType, String providerStatus) {
    String s = (providerStatus == null ? eventType : providerStatus).toUpperCase();
    if (s.contains("REFUND")) {
      return "REFUNDED";
    }
    if (s.contains("VOID") || s.contains("REVERSAL")) {
      return "VOIDED";
    }
    if (s.contains("CAPTURE") || s.contains("SUCCESS") || s.contains("SETTLED")) {
      return "CAPTURED";
    }
    if (s.contains("DECLIN") || s.contains("FAIL")) {
      return "FAILED";
    }
    if (s.contains("AUTHOR")) {
      return "REQUIRES_CAPTURE";
    }
    return null;
  }

  private JsonNode parse(String body) {
    try {
      return objectMapper.readTree(body);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed webhook payload");
    }
  }

  private static Map<String, String> lower(Map<String, String> headers) {
    var out = new java.util.HashMap<String, String>();
    headers.forEach((k, v) -> out.put(k.toLowerCase(), v));
    return out;
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    return null;
  }
}
