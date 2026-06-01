package com.routeshare.trip.repository;

import com.routeshare.trip.domain.TripStatus;
import com.routeshare.trip.entity.TripEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<TripEntity, Long> {
  @Query(
      value =
          """
      SELECT EXISTS(
        SELECT 1 FROM trip.trip t
        JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
        JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
        WHERE t.trip_id = :tripId AND d.app_user_id = :appUserId)
      """,
      nativeQuery = true)
  boolean isOwnedByDriverAppUser(@Param("tripId") long tripId, @Param("appUserId") long appUserId);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select t.status from TripEntity t where t.id = :tripId")
  Optional<TripStatus> lockAndFindStatus(@Param("tripId") long tripId);

  default TripStatus findStatusForUpdate(long tripId) {
    return lockAndFindStatus(tripId).orElseThrow();
  }

  default void updateStatus(long tripId, TripStatus status) {
    TripEntity trip = findById(tripId).orElseThrow();
    trip.setStatus(status);
    java.time.Instant now = java.time.Instant.now();
    if (status == TripStatus.STARTED) {
      trip.setStartedAt(now);
    }
    if (status == TripStatus.COMPLETED) {
      trip.setCompletedAt(now);
    }
    save(trip);
  }
}
