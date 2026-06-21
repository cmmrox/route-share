package com.routeshare.admin.repository;

import com.routeshare.admin.entity.AuditActionEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cross-domain analytics aggregates for admin reports. Bound to {@link AuditActionEntity} only to
 * satisfy Spring Data; every query is native and reads real domain tables. Admin is exempt from the
 * cross-module persistence rule.
 */
public interface AdminAnalyticsRepository extends JpaRepository<AuditActionEntity, Long> {

  @Query(
      value =
          """
      SELECT f.entry_type AS "entryType", COALESCE(SUM(f.amount), 0) AS "amount"
      FROM payment.fare_ledger_entry f
      WHERE f.created_at >= :from AND f.created_at < :to
      GROUP BY f.entry_type
      """,
      nativeQuery = true)
  List<EntryTypeTotalRow> financeTotalsBetween(
      @Param("from") Instant from, @Param("to") Instant to);

  @Query(
      value = "SELECT COUNT(*) FROM booking.booking WHERE created_at >= :from AND created_at < :to",
      nativeQuery = true)
  long bookingsCreatedBetween(@Param("from") Instant from, @Param("to") Instant to);

  @Query(
      value =
          """
      SELECT COUNT(*) FROM booking.booking
      WHERE status = 'COMPLETED' AND created_at >= :from AND created_at < :to
      """,
      nativeQuery = true)
  long bookingsCompletedBetween(@Param("from") Instant from, @Param("to") Instant to);

  @Query(
      value =
          """
      SELECT COUNT(*) FROM trip.trip
      WHERE completed_at IS NOT NULL AND completed_at >= :from AND completed_at < :to
      """,
      nativeQuery = true)
  long tripsCompletedBetween(@Param("from") Instant from, @Param("to") Instant to);

  @Query(
      value =
          "SELECT COUNT(*) FROM identity.app_user WHERE created_at >= :from AND created_at < :to",
      nativeQuery = true)
  long newUsersBetween(@Param("from") Instant from, @Param("to") Instant to);

  interface EntryTypeTotalRow {
    String getEntryType();

    BigDecimal getAmount();
  }
}
