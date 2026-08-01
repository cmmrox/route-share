package com.routeshare.admin.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.payment.service.PaymentReconciliationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authorisations that were never resolved, and gateway calls whose outcome is unknown. */
@RestController
@RequestMapping("/api/v1/admin/payments/reconciliation")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','FINANCE_ADMIN')")
@RequiredArgsConstructor
public class AdminPaymentReconciliationController {
  private final PaymentReconciliationService reconciliation;

  @GetMapping
  ApiResponse<List<PaymentReconciliationService.StuckPayment>> stuck() {
    return ApiResponse.ok(reconciliation.findStuck());
  }
}
