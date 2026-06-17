package com.routeshare.payment.service;

import java.util.Map;

public interface PaymentWebhookService {
  /**
   * Verifies and processes a Cybersource webhook notification. Idempotent per provider event id.
   */
  void handleCybersource(String rawBody, Map<String, String> headers);
}
