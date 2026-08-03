package com.routeshare.payment.repository;

import com.routeshare.payment.entity.FareLedgerEntryEntity;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FareLedgerRepository extends JpaRepository<FareLedgerEntryEntity, Long> {
  @Modifying
  @Query(
      value =
          """
      INSERT INTO payment.fare_ledger_entry(booking_id, entry_type, amount, currency)
      VALUES (:bookingId, :entryType, :amount, :currency)
      ON CONFLICT (booking_id, entry_type, source_key) DO NOTHING
      """,
      nativeQuery = true)
  int insertIfAbsent(
      @Param("bookingId") long bookingId,
      @Param("amount") BigDecimal amount,
      @Param("currency") String currency,
      @Param("entryType") String entryType);

  @Query(
      value =
          """
      SELECT entry_type AS "entryType", amount AS "amount", currency AS "currency"
      FROM payment.fare_ledger_entry
      WHERE booking_id = :bookingId
      ORDER BY fare_ledger_entry_id ASC
      """,
      nativeQuery = true)
  List<FareLedgerRow> findRowsByBookingId(@Param("bookingId") long bookingId);

  default void recordEstimateIfAbsent(
      long bookingId, BigDecimal amount, String currency, String entryType) {
    insertIfAbsent(bookingId, amount, currency, entryType);
  }

  default void recordPaymentLifecycleIfAbsent(
      long bookingId, String entryType, BigDecimal amount, String currency) {
    insertIfAbsent(bookingId, amount, currency, entryType);
  }

  @Modifying
  @Query(
      value =
          """
      INSERT INTO payment.fare_ledger_entry
          (booking_id, entry_type, amount, currency, source_key)
      VALUES (:bookingId, 'REFERRAL_PAYOUT', :amount, :currency, :sourceKey)
      ON CONFLICT (booking_id, entry_type, source_key) DO NOTHING
      """,
      nativeQuery = true)
  int insertReferralPayoutIfAbsent(
      @Param("bookingId") long bookingId,
      @Param("amount") BigDecimal amount,
      @Param("currency") String currency,
      @Param("sourceKey") String sourceKey);

  @Query(
      value =
          """
      SELECT f.booking_id AS "bookingId", f.entry_type AS "entryType", f.amount AS "amount",
             f.currency AS "currency", f.created_at AS "createdAt"
      FROM payment.fare_ledger_entry f
      WHERE f.entry_type IN (:entryTypes)
      ORDER BY f.created_at DESC
      LIMIT 300
      """,
      nativeQuery = true)
  List<FareLedgerAdminRow> findRowsByTypes(
      @Param("entryTypes") java.util.Collection<String> entryTypes);

  @Query(
      value =
          """
      SELECT COALESCE(SUM(f.amount), 0) AS "amount"
      FROM payment.fare_ledger_entry f
      JOIN booking.booking b ON b.booking_id = f.booking_id
      JOIN routing.route_plan r ON r.route_plan_id = b.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      WHERE d.app_user_id = :driverAppUserId
        AND f.entry_type IN ('PAYMENT_CAPTURED','CASH_COLLECTED','FARE_ADJUSTMENT_REQUESTED')
      """,
      nativeQuery = true)
  BigDecimal sumDriverGrossEarnings(@Param("driverAppUserId") long driverAppUserId);

  /**
   * The commission actually taken from this driver's trips. Summed from the ledger rather than
   * recomputed from a rate, so the summary can never disagree with the rows beneath it.
   */
  @Query(
      value =
          """
      SELECT COALESCE(SUM(f.amount), 0) AS "amount"
      FROM payment.fare_ledger_entry f
      JOIN booking.booking b ON b.booking_id = f.booking_id
      JOIN routing.route_plan r ON r.route_plan_id = b.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      WHERE d.app_user_id = :driverAppUserId
        AND f.entry_type = 'PLATFORM_COMMISSION'
      """,
      nativeQuery = true)
  BigDecimal sumDriverCommission(@Param("driverAppUserId") long driverAppUserId);

  @Query(
      value =
          """
      SELECT f.booking_id AS "bookingId", f.entry_type AS "entryType", f.amount AS "amount",
             f.currency AS "currency", f.created_at AS "createdAt"
      FROM payment.fare_ledger_entry f
      JOIN booking.booking b ON b.booking_id = f.booking_id
      JOIN routing.route_plan r ON r.route_plan_id = b.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      WHERE d.app_user_id = :driverAppUserId
      ORDER BY f.created_at DESC
      LIMIT 200
      """,
      nativeQuery = true)
  List<FareLedgerAdminRow> findDriverLedgerRows(@Param("driverAppUserId") long driverAppUserId);

  /** Total DRIVER_EARNING accrued per driver app user (for settlement balances). */
  @Query(
      value =
          """
      SELECT d.app_user_id AS "driverAppUserId", COALESCE(SUM(f.amount), 0) AS "amount"
      FROM payment.fare_ledger_entry f
      JOIN booking.booking b ON b.booking_id = f.booking_id
      JOIN routing.route_plan r ON r.route_plan_id = b.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      WHERE f.entry_type = 'DRIVER_EARNING'
      GROUP BY d.app_user_id
      """,
      nativeQuery = true)
  List<DriverEarningRow> sumDriverEarningsGrouped();

  interface DriverEarningRow {
    Long getDriverAppUserId();

    BigDecimal getAmount();
  }

  interface FareLedgerAdminRow {
    Long getBookingId();

    String getEntryType();

    BigDecimal getAmount();

    String getCurrency();

    java.time.Instant getCreatedAt();
  }

  interface FareLedgerRow {
    String getEntryType();

    BigDecimal getAmount();

    String getCurrency();
  }
}
