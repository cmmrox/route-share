package com.routeshare.trip.repository;

import com.routeshare.trip.entity.TripStartWindowEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripStartWindowRepository extends JpaRepository<TripStartWindowEntity, Long> {

  Optional<TripStartWindowEntity> findByTripId(long tripId);

  /**
   * What the sweeper claims. The pessimistic write lock is the reason a lapsed ShedLock cannot
   * cause a double auto-cancel: two instances may both find the row, but only one may transition
   * it, and the loser re-reads it already resolved.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT w FROM TripStartWindowEntity w
       WHERE w.resolvedAt IS NULL
         AND COALESCE(w.extendedExpiresAt, w.bufferExpiresAt) <= :now
       ORDER BY COALESCE(w.extendedExpiresAt, w.bufferExpiresAt) ASC
      """)
  List<TripStartWindowEntity> claimExpired(@Param("now") Instant now, Pageable page);

  /** The driver a missed start is recorded against — the person, not the driver profile. */
  @Query(
      value =
          """
          SELECT dp.app_user_id
            FROM trip.trip t
            JOIN routing.route_plan rp ON rp.route_plan_id = t.route_plan_id
            JOIN driver.driver_profile dp ON dp.driver_profile_id = rp.driver_profile_id
           WHERE t.trip_id = :tripId
          """,
      nativeQuery = true)
  Optional<Long> findDriverAppUserId(@Param("tripId") long tripId);

  /** Every booking still holding a seat on this trip: each one's hold has to be released. */
  @Query(
      value =
          """
          SELECT b.booking_id
            FROM booking.booking b
            JOIN trip.trip t ON t.route_occurrence_id = b.route_occurrence_id
           WHERE t.trip_id = :tripId
             AND b.status NOT IN ('CANCELLED', 'REJECTED', 'COMPLETED')
          """,
      nativeQuery = true)
  List<Long> findLiveBookingIds(@Param("tripId") long tripId);
}
