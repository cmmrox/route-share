package com.routeshare.payment.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.payment.dto.request.PaymentIntentRequest;
import com.routeshare.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@PreAuthorize("isAuthenticated()")
public class PaymentController {
  private final PaymentService payments;

  public PaymentController(PaymentService payments) {
    this.payments = payments;
  }

  @PostMapping("/intents")
  ApiResponse<Map<String, Object>> create(@Valid @RequestBody PaymentIntentRequest req) {
    return ApiResponse.ok(payments.createIntent(req));
  }
}
