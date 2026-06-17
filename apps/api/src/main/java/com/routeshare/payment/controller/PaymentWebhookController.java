package com.routeshare.payment.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.payment.service.PaymentWebhookService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound provider webhooks. Public (no JWT) but authenticated by provider signature inside the
 * service; see {@link com.routeshare.payment.service.impl.PaymentWebhookServiceImpl}.
 */
@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {
  private final PaymentWebhookService service;

  public PaymentWebhookController(PaymentWebhookService service) {
    this.service = service;
  }

  @PostMapping("/cybersource")
  ApiResponse<Map<String, Object>> cybersource(
      @RequestBody(required = false) String rawBody, @RequestHeader Map<String, String> headers) {
    service.handleCybersource(rawBody, headers);
    return ApiResponse.ok(Map.of("received", true));
  }
}
