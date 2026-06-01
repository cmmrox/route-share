package com.routeshare.trip.repository;

import com.routeshare.trip.domain.PassengerTripStatus;
import com.routeshare.trip.entity.PassengerTripStateEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PassengerTripStateRepository
    extends JpaRepository<PassengerTripStateEntity, Long> {
  @Modifying
  @Query(
      value =
          """
      INSERT INTO trip.passenger_trip_state(
        trip_id, booking_id, route_occurrence_id, passenger_app_user_id, status)
      SELECT t.trip_id, b.booking_id, b.route_occurrence_id, b.passenger_app_user_id, 'WAITING_PICKUP'
      FROM trip.trip t
      JOIN booking.booking b
        ON b.route_plan_id = t.route_plan_id
       AND b.route_occurrence_id = t.route_occurrence_id
      WHERE t.trip_id = :tripId
        AND b.booking_id = :bookingId
        AND b.status = 'CONFIRMED'
      ON CONFLICT (trip_id, booking_id) DO NOTHING
      """,
      nativeQuery = true)
  int ensureWaitingPickupStateForConfirmedBooking(
      @Param("tripId") long tripId, @Param("bookingId") long bookingId);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select s.status from PassengerTripStateEntity s where s.tripId = :tripId and s.bookingId = :bookingId")
  Optional<PassengerTripStatus> findStatusForUpdate(
      @Param("tripId") long tripId, @Param("bookingId") long bookingId);

  @Modifying
  @Query(
      value =
          """
      UPDATE trip.passenger_trip_state
      SET status = :status,
          boarded_at = CASE WHEN :status = 'BOARDED' THEN COALESCE(boarded_at, now()) ELSE boarded_at END,
          no_show_at = CASE WHEN :status = 'NO_SHOW' THEN COALESCE(no_show_at, now()) ELSE no_show_at END,
          dropped_off_at = CASE WHEN :status = 'DROPPED_OFF' THEN COALESCE(dropped_off_at, now()) ELSE dropped_off_at END,
          updated_at = now()
      WHERE trip_id = :tripId
        AND booking_id = :bookingId
      """,
      nativeQuery = true)
  int updateStatusValue(
      @Param("tripId") long tripId,
      @Param("bookingId") long bookingId,
      @Param("status") String status);

  default int updateStatus(long tripId, long bookingId, PassengerTripStatus status) {
    return updateStatusValue(tripId, bookingId, status.name());
  }
}
