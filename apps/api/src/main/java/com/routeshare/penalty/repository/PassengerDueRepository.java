package com.routeshare.penalty.repository;

import com.routeshare.penalty.entity.PassengerDueEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PassengerDueRepository extends JpaRepository<PassengerDueEntity, Long> {

  Optional<PassengerDueEntity> findByPenaltyId(long penaltyId);

  List<PassengerDueEntity> findByAppUserIdOrderByIdDesc(long appUserId);

  @Query(
      """
      SELECT d FROM PassengerDueEntity d
       WHERE d.appUserId = :appUserId AND d.status = 'OUTSTANDING'
       ORDER BY d.id ASC
      """)
  List<PassengerDueEntity> findOutstanding(@Param("appUserId") long appUserId);

  /** The dues a booking carried, for the receipt and for settling them when it captures. */
  List<PassengerDueEntity> findBySettledBookingId(long settledBookingId);

  /**
   * What the platform is still owed by cash passengers. A rising total means penalties are being
   * assessed and not recovered, which is a business problem long before it is a bug.
   */
  @Query(
      """
      SELECT COALESCE(SUM(d.amount), 0) FROM PassengerDueEntity d WHERE d.status = 'OUTSTANDING'
      """)
  BigDecimal sumOutstanding();
}
