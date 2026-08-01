package com.routeshare.admin.service.impl;

import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.admin.service.AdminFinanceService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.finance.dto.CommissionRuleRequest;
import com.routeshare.finance.dto.CommissionRuleResponse;
import com.routeshare.finance.dto.DriverBalanceResponse;
import com.routeshare.finance.dto.FarePolicyRequest;
import com.routeshare.finance.dto.FarePolicyResponse;
import com.routeshare.finance.dto.FinanceAdjustmentRequest;
import com.routeshare.finance.dto.FinanceAdjustmentResponse;
import com.routeshare.finance.dto.PayoutBatchRequest;
import com.routeshare.finance.dto.PayoutBatchResponse;
import com.routeshare.finance.entity.CommissionRuleEntity;
import com.routeshare.finance.entity.FarePolicyEntity;
import com.routeshare.finance.entity.FinanceAdjustmentEntity;
import com.routeshare.finance.entity.PayoutBatchEntity;
import com.routeshare.finance.entity.PayoutBatchItemEntity;
import com.routeshare.finance.repository.CommissionRuleRepository;
import com.routeshare.finance.repository.FarePolicyRepository;
import com.routeshare.finance.repository.FinanceAdjustmentRepository;
import com.routeshare.finance.repository.PayoutBatchItemRepository;
import com.routeshare.finance.repository.PayoutBatchRepository;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.payment.repository.FareLedgerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminFinanceServiceImpl implements AdminFinanceService {
  private static final String CURRENCY = "LKR";
  private static final int MAX_LIMIT = 300;

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final CommissionRuleRepository commissionRules;
  private final FarePolicyRepository farePolicies;
  private final FinanceAdjustmentRepository adjustments;
  private final PayoutBatchRepository payoutBatches;
  private final PayoutBatchItemRepository payoutItems;
  private final FareLedgerRepository fareLedger;
  private final AdminAuditService audit;

  // ---- commission rules ----
  @Override
  @Transactional(readOnly = true)
  public List<CommissionRuleResponse> listCommissionRules() {
    return commissionRules.findAllByOrderByIdDesc().stream().map(this::toRule).toList();
  }

  @Override
  @Transactional
  public CommissionRuleResponse createCommissionRule(CommissionRuleRequest req) {
    var saved =
        commissionRules.save(CommissionRuleEntity.of(req.scope(), req.scopeRef(), req.rate()));
    audit.record("COMMISSION_RULE_CREATED", "COMMISSION_RULE", String.valueOf(saved.getId()), null);
    return toRule(saved);
  }

  @Override
  @Transactional
  public CommissionRuleResponse updateCommissionRule(long ruleId, CommissionRuleRequest req) {
    var rule =
        commissionRules
            .findById(ruleId)
            .orElseThrow(() -> new NoSuchElementException("Commission rule not found"));
    if (req.scope() != null) {
      rule.setScope(req.scope());
    }
    rule.setScopeRef(req.scopeRef());
    if (req.rate() != null) {
      rule.setRate(req.rate());
    }
    if (req.active() != null) {
      rule.setActive(req.active());
    }
    rule.setUpdatedAt(Instant.now());
    audit.record("COMMISSION_RULE_UPDATED", "COMMISSION_RULE", String.valueOf(ruleId), null);
    return toRule(rule);
  }

  // ---- fare policies ----
  @Override
  @Transactional(readOnly = true)
  public List<FarePolicyResponse> listFarePolicies() {
    return farePolicies.findAllByOrderByIdDesc().stream().map(this::toPolicy).toList();
  }

  @Override
  @Transactional
  public FarePolicyResponse createFarePolicy(FarePolicyRequest req) {
    var policy = FarePolicyEntity.blank();
    applyPolicy(policy, req);
    audit.record("FARE_POLICY_CREATED", "FARE_POLICY", null, null);
    return toPolicy(farePolicies.save(policy));
  }

  @Override
  @Transactional
  public FarePolicyResponse updateFarePolicy(long policyId, FarePolicyRequest req) {
    var policy =
        farePolicies
            .findById(policyId)
            .orElseThrow(() -> new NoSuchElementException("Fare policy not found"));
    applyPolicy(policy, req);
    audit.record("FARE_POLICY_UPDATED", "FARE_POLICY", String.valueOf(policyId), null);
    return toPolicy(farePolicies.save(policy));
  }

  private void applyPolicy(FarePolicyEntity p, FarePolicyRequest req) {
    p.setName(req.name());
    // Base fare, per-km and per-minute retired with the old model. The band prices the distance;
    // the floor is all that is left to configure here.
    p.setMinFare(req.minFare() == null ? BigDecimal.ZERO : req.minFare());
    p.setCurrency(req.currency() == null || req.currency().isBlank() ? CURRENCY : req.currency());
    if (req.active() != null) {
      p.setActive(req.active());
    }
    p.setUpdatedAt(Instant.now());
  }

  // ---- finance adjustments ----
  @Override
  @Transactional(readOnly = true)
  public List<FinanceAdjustmentResponse> listAdjustments(int limit) {
    return adjustments
        .findAllByOrderByIdDesc(PageRequest.of(0, Math.min(limit <= 0 ? 100 : limit, MAX_LIMIT)))
        .stream()
        .map(this::toAdjustment)
        .toList();
  }

  @Override
  @Transactional
  public FinanceAdjustmentResponse createAdjustment(FinanceAdjustmentRequest req) {
    if (req.amount().signum() == 0) {
      throw new IllegalArgumentException("Adjustment amount cannot be zero");
    }
    var saved =
        adjustments.save(
            FinanceAdjustmentEntity.of(
                req.bookingId(),
                req.driverAppUserId(),
                req.amount(),
                req.currency() == null ? CURRENCY : req.currency(),
                req.reason(),
                currentAdminId()));
    audit.record("FINANCE_ADJUSTMENT", "FINANCE_ADJUSTMENT", String.valueOf(saved.getId()), null);
    return toAdjustment(saved);
  }

  // ---- settlements ----
  @Override
  @Transactional(readOnly = true)
  public List<DriverBalanceResponse> driverBalances() {
    Map<Long, BigDecimal> earned = new LinkedHashMap<>();
    for (var row : fareLedger.sumDriverEarningsGrouped()) {
      earned.merge(row.getDriverAppUserId(), nz(row.getAmount()), BigDecimal::add);
    }
    Map<Long, BigDecimal> paid = new LinkedHashMap<>();
    for (var row : payoutItems.sumPaidByDriver()) {
      paid.merge(row.getDriverAppUserId(), nz(row.getAmount()), BigDecimal::add);
    }
    List<DriverBalanceResponse> result = new ArrayList<>();
    for (var entry : earned.entrySet()) {
      BigDecimal paidOut = paid.getOrDefault(entry.getKey(), BigDecimal.ZERO);
      result.add(
          new DriverBalanceResponse(
              entry.getKey(),
              entry.getValue(),
              paidOut,
              entry.getValue().subtract(paidOut),
              CURRENCY));
    }
    return result;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PayoutBatchResponse> listPayoutBatches(int limit) {
    return payoutBatches
        .findAllByOrderByIdDesc(PageRequest.of(0, Math.min(limit <= 0 ? 50 : limit, MAX_LIMIT)))
        .stream()
        .map(b -> toBatch(b, false))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PayoutBatchResponse getPayoutBatch(long batchId) {
    return toBatch(requireBatch(batchId), true);
  }

  @Override
  @Transactional
  public PayoutBatchResponse createPayoutBatch(PayoutBatchRequest req) {
    var batch = payoutBatches.save(PayoutBatchEntity.open(currentAdminId(), req.note()));
    BigDecimal total = BigDecimal.ZERO;
    for (var item : req.items()) {
      payoutItems.save(
          PayoutBatchItemEntity.of(
              batch.getId(),
              item.driverAppUserId(),
              item.amount(),
              item.currency() == null ? CURRENCY : item.currency()));
      total = total.add(item.amount());
    }
    batch.setTotalAmount(total);
    payoutBatches.save(batch);
    audit.record("PAYOUT_BATCH_CREATED", "PAYOUT_BATCH", String.valueOf(batch.getId()), null);
    return toBatch(batch, true);
  }

  @Override
  @Transactional
  public PayoutBatchResponse markPayoutPaid(long batchId) {
    var batch = requireBatch(batchId);
    if (!PayoutBatchEntity.OPEN.equals(batch.getStatus())) {
      throw new IllegalStateException("Only OPEN payout batches can be marked paid");
    }
    batch.setStatus(PayoutBatchEntity.PAID);
    batch.setPaidAt(Instant.now());
    audit.record("PAYOUT_BATCH_PAID", "PAYOUT_BATCH", String.valueOf(batchId), null);
    return toBatch(batch, true);
  }

  private PayoutBatchEntity requireBatch(long batchId) {
    return payoutBatches
        .findById(batchId)
        .orElseThrow(() -> new NoSuchElementException("Payout batch not found"));
  }

  private long currentAdminId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }

  private static BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  private CommissionRuleResponse toRule(CommissionRuleEntity e) {
    return new CommissionRuleResponse(
        e.getId(), e.getScope(), e.getScopeRef(), e.getRate(), e.isActive(), e.getUpdatedAt());
  }

  private FarePolicyResponse toPolicy(FarePolicyEntity e) {
    return new FarePolicyResponse(
        e.getId(), e.getName(), e.getMinFare(), e.getCurrency(), e.isActive(), e.getUpdatedAt());
  }

  private FinanceAdjustmentResponse toAdjustment(FinanceAdjustmentEntity e) {
    return new FinanceAdjustmentResponse(
        e.getId(),
        e.getBookingId(),
        e.getDriverAppUserId(),
        e.getAmount(),
        e.getCurrency(),
        e.getReason(),
        e.getCreatedBy(),
        e.getCreatedAt());
  }

  private PayoutBatchResponse toBatch(PayoutBatchEntity b, boolean withItems) {
    List<PayoutBatchResponse.Item> items =
        withItems
            ? payoutItems.findByPayoutBatchId(b.getId()).stream()
                .map(
                    i ->
                        new PayoutBatchResponse.Item(
                            i.getId(), i.getDriverAppUserId(), i.getAmount(), i.getCurrency()))
                .toList()
            : List.of();
    return new PayoutBatchResponse(
        b.getId(),
        b.getStatus(),
        b.getTotalAmount(),
        b.getCurrency(),
        b.getNote(),
        b.getCreatedAt(),
        b.getPaidAt(),
        items);
  }
}
