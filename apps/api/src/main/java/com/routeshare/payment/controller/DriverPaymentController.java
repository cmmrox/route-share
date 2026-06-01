package com.routeshare.payment.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.payment.dto.request.CashCollectionRequest;
import com.routeshare.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver/bookings")
@PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
public class DriverPaymentController {
  private final PaymentService payments;

  public DriverPaymentController(PaymentService payments) {
    this.payments = payments;
  }

  @PostMapping("/{bookingId}/cash-collected")
  ApiResponse<Map<String, Object>> cashCollected(
      @PathVariable long bookingId, @Valid @RequestBody CashCollectionRequest req) {
    return ApiResponse.ok(payments.recordCashCollected(bookingId, req));
  }
}
