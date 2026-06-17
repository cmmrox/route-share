package com.routeshare.payment.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.payment.dto.request.AddPaymentMethodRequest;
import com.routeshare.payment.dto.response.PaymentMethodResponse;
import com.routeshare.payment.service.PaymentMethodService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passenger/payment-methods")
@PreAuthorize("hasRole('PASSENGER')")
public class PaymentMethodController {
  private final PaymentMethodService service;

  public PaymentMethodController(PaymentMethodService service) {
    this.service = service;
  }

  @GetMapping
  ApiResponse<List<PaymentMethodResponse>> list() {
    return ApiResponse.ok(service.listMine());
  }

  @PostMapping
  ApiResponse<PaymentMethodResponse> add(@Valid @RequestBody AddPaymentMethodRequest req) {
    return ApiResponse.ok(service.add(req));
  }

  @DeleteMapping("/{paymentMethodId}")
  ApiResponse<Void> delete(@PathVariable long paymentMethodId) {
    service.delete(paymentMethodId);
    return ApiResponse.ok(null);
  }

  @PostMapping("/{paymentMethodId}/default")
  ApiResponse<PaymentMethodResponse> setDefault(@PathVariable long paymentMethodId) {
    return ApiResponse.ok(service.setDefault(paymentMethodId));
  }
}
