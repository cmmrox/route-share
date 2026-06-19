package com.routeshare.admin.controller;

import com.routeshare.admin.service.AdminFinanceService;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.finance.dto.CommissionRuleRequest;
import com.routeshare.finance.dto.CommissionRuleResponse;
import com.routeshare.finance.dto.DriverBalanceResponse;
import com.routeshare.finance.dto.FarePolicyRequest;
import com.routeshare.finance.dto.FarePolicyResponse;
import com.routeshare.finance.dto.FinanceAdjustmentRequest;
import com.routeshare.finance.dto.FinanceAdjustmentResponse;
import com.routeshare.finance.dto.PayoutBatchRequest;
import com.routeshare.finance.dto.PayoutBatchResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','FINANCE_ADMIN')")
public class AdminFinanceController {
  private final AdminFinanceService service;

  public AdminFinanceController(AdminFinanceService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/admin/commission-rules")
  ApiResponse<List<CommissionRuleResponse>> commissionRules() {
    return ApiResponse.ok(service.listCommissionRules());
  }

  @PostMapping("/api/v1/admin/commission-rules")
  ApiResponse<CommissionRuleResponse> createCommissionRule(
      @Valid @RequestBody CommissionRuleRequest req) {
    return ApiResponse.ok(service.createCommissionRule(req));
  }

  @PutMapping("/api/v1/admin/commission-rules/{ruleId}")
  ApiResponse<CommissionRuleResponse> updateCommissionRule(
      @PathVariable long ruleId, @Valid @RequestBody CommissionRuleRequest req) {
    return ApiResponse.ok(service.updateCommissionRule(ruleId, req));
  }

  @GetMapping("/api/v1/admin/fare-policies")
  ApiResponse<List<FarePolicyResponse>> farePolicies() {
    return ApiResponse.ok(service.listFarePolicies());
  }

  @PostMapping("/api/v1/admin/fare-policies")
  ApiResponse<FarePolicyResponse> createFarePolicy(@Valid @RequestBody FarePolicyRequest req) {
    return ApiResponse.ok(service.createFarePolicy(req));
  }

  @PutMapping("/api/v1/admin/fare-policies/{policyId}")
  ApiResponse<FarePolicyResponse> updateFarePolicy(
      @PathVariable long policyId, @Valid @RequestBody FarePolicyRequest req) {
    return ApiResponse.ok(service.updateFarePolicy(policyId, req));
  }

  @GetMapping("/api/v1/admin/settlements/driver-balances")
  ApiResponse<List<DriverBalanceResponse>> driverBalances() {
    return ApiResponse.ok(service.driverBalances());
  }

  @GetMapping("/api/v1/admin/settlements/payout-batches")
  ApiResponse<List<PayoutBatchResponse>> payoutBatches(
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ApiResponse.ok(service.listPayoutBatches(limit));
  }

  @GetMapping("/api/v1/admin/settlements/payout-batches/{batchId}")
  ApiResponse<PayoutBatchResponse> payoutBatch(@PathVariable long batchId) {
    return ApiResponse.ok(service.getPayoutBatch(batchId));
  }

  @PostMapping("/api/v1/admin/settlements/payout-batches")
  ApiResponse<PayoutBatchResponse> createPayoutBatch(@Valid @RequestBody PayoutBatchRequest req) {
    return ApiResponse.ok(service.createPayoutBatch(req));
  }

  @PostMapping("/api/v1/admin/settlements/payout-batches/{batchId}/mark-paid")
  ApiResponse<PayoutBatchResponse> markPaid(@PathVariable long batchId) {
    return ApiResponse.ok(service.markPayoutPaid(batchId));
  }

  @GetMapping("/api/v1/admin/finance/adjustments")
  ApiResponse<List<FinanceAdjustmentResponse>> adjustments(
      @RequestParam(name = "limit", defaultValue = "100") int limit) {
    return ApiResponse.ok(service.listAdjustments(limit));
  }

  @PostMapping("/api/v1/admin/finance/adjustments")
  ApiResponse<FinanceAdjustmentResponse> createAdjustment(
      @Valid @RequestBody FinanceAdjustmentRequest req) {
    return ApiResponse.ok(service.createAdjustment(req));
  }
}
