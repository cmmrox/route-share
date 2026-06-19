package com.routeshare.finance.repository;

import com.routeshare.finance.entity.PayoutBatchItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PayoutBatchItemRepository extends JpaRepository<PayoutBatchItemEntity, Long> {
  List<PayoutBatchItemEntity> findByPayoutBatchId(long payoutBatchId);

  /** Total already paid out per driver across PAID batches. */
  @Query(
      value =
          """
      SELECT pbi.driver_app_user_id AS "driverAppUserId", COALESCE(SUM(pbi.amount), 0) AS "amount"
      FROM finance.payout_batch_item pbi
      JOIN finance.payout_batch pb ON pb.payout_batch_id = pbi.payout_batch_id
      WHERE pb.status = 'PAID'
      GROUP BY pbi.driver_app_user_id
      """,
      nativeQuery = true)
  List<DriverAmountRow> sumPaidByDriver();

  interface DriverAmountRow {
    Long getDriverAppUserId();

    java.math.BigDecimal getAmount();
  }
}
