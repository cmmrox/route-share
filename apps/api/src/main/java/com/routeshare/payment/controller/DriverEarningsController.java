package com.routeshare.payment.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.payment.dto.request.FareAdjustmentRequest;
import com.routeshare.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
public class DriverEarningsController {
  private final PaymentService payments;

  public DriverEarningsController(PaymentService payments) {
    this.payments = payments;
  }

  @PostMapping("/api/v1/driver/bookings/{bookingId}/fare-adjustment-request")
  public ApiResponse<Map<String, Object>> fareAdjustment(
      @PathVariable long bookingId, @Valid @RequestBody FareAdjustmentRequest req) {
    return ApiResponse.ok(payments.requestFareAdjustment(bookingId, req));
  }

  @GetMapping("/api/v1/driver/earnings/summary")
  public ApiResponse<Map<String, Object>> summary() {
    return ApiResponse.ok(payments.driverEarningsSummary());
  }

  @GetMapping("/api/v1/driver/earnings/transactions")
  public ApiResponse<List<Map<String, Object>>> transactions() {
    return ApiResponse.ok(payments.driverEarningsTransactions());
  }
}
