package com.routeshare.admin.service;

import com.routeshare.finance.dto.CommissionRuleRequest;
import com.routeshare.finance.dto.CommissionRuleResponse;
import com.routeshare.finance.dto.DriverBalanceResponse;
import com.routeshare.finance.dto.FarePolicyRequest;
import com.routeshare.finance.dto.FarePolicyResponse;
import com.routeshare.finance.dto.FinanceAdjustmentRequest;
import com.routeshare.finance.dto.FinanceAdjustmentResponse;
import com.routeshare.finance.dto.PayoutBatchRequest;
import com.routeshare.finance.dto.PayoutBatchResponse;
import java.util.List;

public interface AdminFinanceService {
  List<CommissionRuleResponse> listCommissionRules();

  CommissionRuleResponse createCommissionRule(CommissionRuleRequest req);

  CommissionRuleResponse updateCommissionRule(long ruleId, CommissionRuleRequest req);

  List<FarePolicyResponse> listFarePolicies();

  FarePolicyResponse createFarePolicy(FarePolicyRequest req);

  FarePolicyResponse updateFarePolicy(long policyId, FarePolicyRequest req);

  List<FinanceAdjustmentResponse> listAdjustments(int limit);

  FinanceAdjustmentResponse createAdjustment(FinanceAdjustmentRequest req);

  List<DriverBalanceResponse> driverBalances();

  List<PayoutBatchResponse> listPayoutBatches(int limit);

  PayoutBatchResponse getPayoutBatch(long batchId);

  PayoutBatchResponse createPayoutBatch(PayoutBatchRequest req);

  PayoutBatchResponse markPayoutPaid(long batchId);
}
