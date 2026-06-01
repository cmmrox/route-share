package com.routeshare.payment.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.payment.dto.request.PaymentLifecycleRequest;
import com.routeshare.payment.service.PaymentService;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminPaymentController {
  private final PaymentService payments;

  public AdminPaymentController(PaymentService payments) {
    this.payments = payments;
  }

  @GetMapping("/payments")
  public ApiResponse<List<Map<String, Object>>> list() {
    return ApiResponse.ok(payments.adminPayments());
  }

  @GetMapping("/payments/{paymentIntentId}")
  public ApiResponse<Map<String, Object>> detail(@PathVariable long paymentIntentId) {
    return ApiResponse.ok(payments.adminPaymentDetail(paymentIntentId));
  }

  @GetMapping("/payments/{paymentIntentId}/events")
  public ApiResponse<List<Map<String, Object>>> events(@PathVariable long paymentIntentId) {
    return ApiResponse.ok(payments.adminPaymentEvents(paymentIntentId));
  }

  @GetMapping("/cash-collections")
  public ApiResponse<List<Map<String, Object>>> cashCollections() {
    return ApiResponse.ok(payments.adminCashCollections());
  }

  @PostMapping("/payments/{paymentIntentId}/capture")
  public ApiResponse<Map<String, Object>> capture(
      @PathVariable long paymentIntentId,
      @RequestBody(required = false) PaymentLifecycleRequest req) {
    return ApiResponse.ok(
        payments.capture(paymentIntentId, req == null ? new PaymentLifecycleRequest(null) : req));
  }

  @PostMapping("/payments/{paymentIntentId}/void")
  public ApiResponse<Map<String, Object>> voidIntent(
      @PathVariable long paymentIntentId,
      @RequestBody(required = false) PaymentLifecycleRequest req) {
    return ApiResponse.ok(
        payments.voidIntent(
            paymentIntentId, req == null ? new PaymentLifecycleRequest(null) : req));
  }

  @PostMapping("/payments/{paymentIntentId}/refund")
  public ApiResponse<Map<String, Object>> refund(
      @PathVariable long paymentIntentId,
      @RequestBody(required = false) PaymentLifecycleRequest req) {
    return ApiResponse.ok(
        payments.refund(paymentIntentId, req == null ? new PaymentLifecycleRequest(null) : req));
  }
}
