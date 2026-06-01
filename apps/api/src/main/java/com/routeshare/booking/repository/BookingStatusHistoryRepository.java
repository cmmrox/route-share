package com.routeshare.booking.repository;

import com.routeshare.booking.entity.BookingStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingStatusHistoryRepository
    extends JpaRepository<BookingStatusHistoryEntity, Long> {
  @Modifying
  @Query(
      value =
          """
      INSERT INTO booking.booking_status_history(
        booking_id, from_status, to_status, changed_by_app_user_id, reason)
      VALUES (:bookingId, NULL, :toStatus, :changedByAppUserId, :reason)
      """,
      nativeQuery = true)
  void recordInitialStatus(
      @Param("bookingId") long bookingId,
      @Param("toStatus") String toStatus,
      @Param("changedByAppUserId") long changedByAppUserId,
      @Param("reason") String reason);
}
