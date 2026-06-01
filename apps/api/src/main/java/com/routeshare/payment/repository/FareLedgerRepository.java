package com.routeshare.payment.repository;

import com.routeshare.payment.entity.FareLedgerEntryEntity;
import java.math.BigDecimal;
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
      ON CONFLICT (booking_id, entry_type) DO NOTHING
      """,
      nativeQuery = true)
  int insertIfAbsent(
      @Param("bookingId") long bookingId,
      @Param("amount") BigDecimal amount,
      @Param("currency") String currency,
      @Param("entryType") String entryType);

  default void recordEstimateIfAbsent(
      long bookingId, BigDecimal amount, String currency, String entryType) {
    insertIfAbsent(bookingId, amount, currency, entryType);
  }
}
