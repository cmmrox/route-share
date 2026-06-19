package com.routeshare.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.finance.dto.PayoutBatchRequest;
import com.routeshare.finance.entity.PayoutBatchEntity;
import com.routeshare.finance.entity.PayoutBatchItemEntity;
import com.routeshare.finance.repository.CommissionRuleRepository;
import com.routeshare.finance.repository.FarePolicyRepository;
import com.routeshare.finance.repository.FinanceAdjustmentRepository;
import com.routeshare.finance.repository.PayoutBatchItemRepository;
import com.routeshare.finance.repository.PayoutBatchRepository;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.payment.repository.FareLedgerRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminFinanceServiceImplTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final CommissionRuleRepository commissionRules = mock(CommissionRuleRepository.class);
  private final FarePolicyRepository farePolicies = mock(FarePolicyRepository.class);
  private final FinanceAdjustmentRepository adjustments = mock(FinanceAdjustmentRepository.class);
  private final PayoutBatchRepository payoutBatches = mock(PayoutBatchRepository.class);
  private final PayoutBatchItemRepository payoutItems = mock(PayoutBatchItemRepository.class);
  private final FareLedgerRepository fareLedger = mock(FareLedgerRepository.class);
  private final AdminAuditService audit = mock(AdminAuditService.class);
  private final AdminFinanceServiceImpl service =
      new AdminFinanceServiceImpl(
          current,
          identityFacade,
          commissionRules,
          farePolicies,
          adjustments,
          payoutBatches,
          payoutItems,
          fareLedger,
          audit);

  @BeforeEach
  void setUp() {
    var admin = new CurrentUser("a", "a@test", null, "Admin", Set.of("FINANCE_ADMIN"));
    when(current.requireCurrentUser()).thenReturn(admin);
    when(identityFacade.upsertFromToken(admin))
        .thenReturn(new AppUser(99L, UUID.randomUUID(), "a", "a@test", null, "Admin", "ACTIVE"));
  }

  @Test
  void createPayoutBatchSumsItemsIntoTotalAndAudits() {
    when(payoutBatches.save(any(PayoutBatchEntity.class)))
        .thenAnswer(
            inv -> {
              PayoutBatchEntity b = inv.getArgument(0);
              if (b.getId() == null) {
                b.setId(20L);
              }
              return b;
            });
    when(payoutItems.save(any(PayoutBatchItemEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(payoutItems.findByPayoutBatchId(20L)).thenReturn(List.of());

    var req =
        new PayoutBatchRequest(
            "June payouts",
            List.of(
                new PayoutBatchRequest.Item(1L, new BigDecimal("1000.00"), "LKR"),
                new PayoutBatchRequest.Item(2L, new BigDecimal("500.00"), "LKR")));

    var res = service.createPayoutBatch(req);

    assertThat(res.totalAmount()).isEqualByComparingTo("1500.00");
    assertThat(res.status()).isEqualTo("OPEN");
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void markPayoutPaidRejectsNonOpenBatch() {
    var batch = PayoutBatchEntity.open(99L, "x");
    batch.setId(20L);
    batch.setStatus(PayoutBatchEntity.PAID);
    when(payoutBatches.findById(20L)).thenReturn(Optional.of(batch));

    assertThatThrownBy(() -> service.markPayoutPaid(20L)).isInstanceOf(IllegalStateException.class);
  }
}
