package com.routeshare.location.repository;

import com.routeshare.location.entity.ApproachSessionEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ApproachSessionRepository extends JpaRepository<ApproachSessionEntity, Long> {
  @Query(
      value =
          """
          SELECT b.booking_id AS "bookingId",
                 ST_Distance(p.last_position::geography, COALESCE(pp.position, b.pickup)::geography)
                     AS "distanceMeters"
            FROM location.trip_progress p
            JOIN trip.trip t ON t.trip_id = p.trip_id
            JOIN booking.booking b ON b.route_plan_id = t.route_plan_id
                 AND (b.route_occurrence_id = t.route_occurrence_id OR t.route_occurrence_id IS NULL)
            LEFT JOIN routing.pickup_point pp ON pp.pickup_point_id = b.pickup_point_id
           WHERE p.trip_id = :tripId
             AND b.status = 'CONFIRMED'
             AND b.pickup_route_fraction >= p.route_fraction
             AND NOT EXISTS (
                 SELECT 1 FROM trip.passenger_trip_state ps
                  WHERE ps.booking_id = b.booking_id
                    AND ps.status IN ('BOARDED','NO_SHOW','DROPPED_OFF'))
           ORDER BY b.pickup_route_fraction, b.booking_id
           LIMIT 1
          """,
      nativeQuery = true)
  Optional<NextPickupRow> nextPickup(@Param("tripId") long tripId);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO location.approach_session(trip_id, booking_id)
          SELECT :tripId, :bookingId
           WHERE NOT EXISTS (
               SELECT 1 FROM location.approach_session
                WHERE booking_id = :bookingId AND closed_at IS NULL)
          """,
      nativeQuery = true)
  int open(@Param("tripId") long tripId, @Param("bookingId") long bookingId);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          UPDATE location.approach_session a
             SET rider_position = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326),
                 rider_position_at = :now
            FROM booking.booking b
           WHERE a.booking_id = :bookingId
             AND b.booking_id = a.booking_id
             AND b.passenger_app_user_id = :appUserId
             AND a.closed_at IS NULL
          """,
      nativeQuery = true)
  int updateRiderPosition(
      @Param("bookingId") long bookingId,
      @Param("appUserId") long appUserId,
      @Param("lat") double latitude,
      @Param("lng") double longitude,
      @Param("now") Instant now);

  @Query(
      value =
          """
          SELECT a.booking_id AS "bookingId", a.trip_id AS "tripId",
                 pp.label AS "pickupLabel", pp.description AS "pickupDescription",
                 pp.side_hint AS "pickupSideHint",
                 ST_Y(COALESCE(pp.position, b.pickup)) AS "pickupLatitude",
                 ST_X(COALESCE(pp.position, b.pickup)) AS "pickupLongitude",
                 ST_Y(a.rider_position) AS "counterpartyLatitude",
                 ST_X(a.rider_position) AS "counterpartyLongitude",
                 a.rider_position_at AS "counterpartyAt",
                 ST_Distance(
                     p.last_position::geography,
                     COALESCE(pp.position, b.pickup)::geography) AS "distanceMeters",
                 v.make AS "vehicleMake", v.color AS "vehicleColour",
                 v.registration_number AS "vehiclePlate",
                 p.speed_mps AS "speedMps"
            FROM location.approach_session a
            JOIN booking.booking b ON b.booking_id = a.booking_id
            JOIN trip.trip t ON t.trip_id = a.trip_id
            JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
            JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
            JOIN vehicle.vehicle v ON v.vehicle_id = r.vehicle_id
            JOIN location.trip_progress p ON p.trip_id = a.trip_id
            LEFT JOIN routing.pickup_point pp ON pp.pickup_point_id = b.pickup_point_id
           WHERE a.trip_id = :tripId AND d.app_user_id = :appUserId AND a.closed_at IS NULL
           ORDER BY a.opened_at DESC LIMIT 1
          """,
      nativeQuery = true)
  Optional<ApproachRow> driverApproach(
      @Param("tripId") long tripId, @Param("appUserId") long appUserId);

  @Query(
      value =
          """
          SELECT a.booking_id AS "bookingId", a.trip_id AS "tripId",
                 pp.label AS "pickupLabel", pp.description AS "pickupDescription",
                 pp.side_hint AS "pickupSideHint",
                 ST_Y(COALESCE(pp.position, b.pickup)) AS "pickupLatitude",
                 ST_X(COALESCE(pp.position, b.pickup)) AS "pickupLongitude",
                 ST_Y(p.last_position) AS "counterpartyLatitude",
                 ST_X(p.last_position) AS "counterpartyLongitude",
                 p.updated_at AS "counterpartyAt",
                 ST_Distance(
                     p.last_position::geography,
                     COALESCE(pp.position, b.pickup)::geography) AS "distanceMeters",
                 v.make AS "vehicleMake", v.color AS "vehicleColour",
                 v.registration_number AS "vehiclePlate",
                 p.speed_mps AS "speedMps"
            FROM location.approach_session a
            JOIN booking.booking b ON b.booking_id = a.booking_id
            JOIN trip.trip t ON t.trip_id = a.trip_id
            JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
            JOIN vehicle.vehicle v ON v.vehicle_id = r.vehicle_id
            JOIN location.trip_progress p ON p.trip_id = a.trip_id
            LEFT JOIN routing.pickup_point pp ON pp.pickup_point_id = b.pickup_point_id
           WHERE a.booking_id = :bookingId
             AND b.passenger_app_user_id = :appUserId
             AND a.closed_at IS NULL
          """,
      nativeQuery = true)
  Optional<ApproachRow> passengerApproach(
      @Param("bookingId") long bookingId, @Param("appUserId") long appUserId);

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
  boolean driverOwnsTrip(@Param("tripId") long tripId, @Param("appUserId") long appUserId);

  @Query(
      value =
          """
          SELECT EXISTS(
              SELECT 1 FROM booking.booking b
               WHERE b.booking_id = :bookingId AND b.passenger_app_user_id = :appUserId)
          """,
      nativeQuery = true)
  boolean passengerOwnsBooking(
      @Param("bookingId") long bookingId, @Param("appUserId") long appUserId);

  @Query(
      value =
          """
          SELECT a.approach_session_id
            FROM location.approach_session a
            JOIN trip.trip t ON t.trip_id = a.trip_id
            LEFT JOIN trip.passenger_trip_state ps ON ps.booking_id = a.booking_id
           WHERE a.closed_at IS NULL
             AND (
                 t.status IN ('COMPLETED','CANCELLED')
                 OR ps.status IN ('BOARDED','NO_SHOW','DROPPED_OFF')
                 OR a.opened_at < :orphanedBefore)
           ORDER BY a.approach_session_id
           LIMIT :limit
          """,
      nativeQuery = true)
  List<Long> staleSessionIds(
      @Param("orphanedBefore") Instant orphanedBefore, @Param("limit") int limit);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          UPDATE location.approach_session
             SET closed_at = :now, rider_position = NULL, rider_position_at = NULL
           WHERE approach_session_id = :id AND closed_at IS NULL
          """,
      nativeQuery = true)
  int closeAndDeleteRiderPosition(@Param("id") long id, @Param("now") Instant now);

  interface NextPickupRow {
    Long getBookingId();

    Double getDistanceMeters();
  }

  interface ApproachRow {
    Long getBookingId();

    Long getTripId();

    String getPickupLabel();

    String getPickupDescription();

    String getPickupSideHint();

    Double getPickupLatitude();

    Double getPickupLongitude();

    Double getCounterpartyLatitude();

    Double getCounterpartyLongitude();

    Instant getCounterpartyAt();

    Double getDistanceMeters();

    String getVehicleMake();

    String getVehicleColour();

    String getVehiclePlate();

    java.math.BigDecimal getSpeedMps();
  }
}
