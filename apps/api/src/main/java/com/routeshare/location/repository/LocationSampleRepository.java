package com.routeshare.location.repository;

import com.routeshare.location.dto.request.LocationUpdateRequest;
import com.routeshare.location.entity.LocationSampleEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface LocationSampleRepository extends JpaRepository<LocationSampleEntity, Long> {
  @Query(
      value =
          """
      SELECT EXISTS(
        SELECT 1 FROM trip.trip t
        JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
        JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
        WHERE t.trip_id = :tripId AND d.driver_profile_id = :driverProfileId
          AND d.app_user_id = :appUserId
          AND t.status IN ('STARTED', 'ARRIVED_PICKUP', 'PASSENGER_ONBOARD'))
      """,
      nativeQuery = true)
  boolean canUpdateTripLocation(
      @Param("tripId") long tripId,
      @Param("driverProfileId") long driverProfileId,
      @Param("appUserId") long appUserId);

  @Query(
      value =
          """
      SELECT d.driver_profile_id
      FROM trip.trip t
      JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      WHERE t.trip_id = :tripId
        AND d.app_user_id = :appUserId
        AND t.status IN ('STARTED', 'ARRIVED_PICKUP', 'PASSENGER_ONBOARD')
      """,
      nativeQuery = true)
  Optional<Long> findDriverProfileIdForActiveTrip(
      @Param("tripId") long tripId, @Param("appUserId") long appUserId);

  @Query(
      value =
          """
      SELECT EXISTS(
        SELECT 1
        FROM trip.trip t
        JOIN booking.booking b ON b.route_plan_id = t.route_plan_id
          AND (b.route_occurrence_id = t.route_occurrence_id OR t.route_occurrence_id IS NULL)
        JOIN passenger.passenger_profile p ON p.passenger_profile_id = b.passenger_profile_id
        WHERE t.trip_id = :tripId AND p.app_user_id = :appUserId
      )
      """,
      nativeQuery = true)
  boolean passengerCanViewTrip(@Param("tripId") long tripId, @Param("appUserId") long appUserId);

  @Query(
      value =
          """
      SELECT
        t.trip_id AS "tripId",
        t.status AS "tripStatus",
        r.origin_label AS "originLabel",
        r.destination_label AS "destinationLabel",
        ro.scheduled_departure_at AS "departureTime"
      FROM trip.trip t
      JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
      LEFT JOIN routing.route_occurrence ro ON ro.route_occurrence_id = t.route_occurrence_id
      WHERE t.trip_id = :tripId
      """,
      nativeQuery = true)
  Optional<PassengerLiveTripRow> findPassengerLiveTrip(@Param("tripId") long tripId);

  @Query(
      value =
          """
      SELECT
        t.trip_id AS "tripId",
        d.driver_profile_id AS "driverProfileId",
        COALESCE(au.display_name, au.email, 'Driver') AS "driverName",
        t.status AS "tripStatus",
        r.origin_label AS "originLabel",
        r.destination_label AS "destinationLabel",
        ro.scheduled_departure_at AS "departureTime"
      FROM trip.trip t
      JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      JOIN identity.app_user au ON au.app_user_id = d.app_user_id
      LEFT JOIN routing.route_occurrence ro ON ro.route_occurrence_id = t.route_occurrence_id
      WHERE t.status IN ('STARTED', 'ARRIVED_PICKUP', 'PASSENGER_ONBOARD')
      ORDER BY COALESCE(ro.scheduled_departure_at, t.started_at) DESC
      LIMIT :limit
      """,
      nativeQuery = true)
  List<AdminLiveTripRow> findAdminLiveTrips(@Param("limit") int limit);

  @Transactional
  @Modifying
  @Query(
      value =
          """
      INSERT INTO location.location_sample(
        trip_id, driver_profile_id, point, accuracy_m, speed_mps, bearing_degrees, device_recorded_at)
      VALUES (:tripId, :driverProfileId, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326),
        :accuracyMeters, :speedMps, :bearingDegrees, :deviceRecordedAt)
      """,
      nativeQuery = true)
  void insertSample(
      @Param("tripId") Long tripId,
      @Param("driverProfileId") Long driverProfileId,
      @Param("lng") Double longitude,
      @Param("lat") Double latitude,
      @Param("accuracyMeters") Double accuracyMeters,
      @Param("speedMps") Double speedMps,
      @Param("bearingDegrees") Double bearingDegrees,
      @Param("deviceRecordedAt") Instant deviceRecordedAt);

  @Transactional
  @Modifying
  @Query(
      value =
          """
      INSERT INTO location.location_event_outbox(trip_id, driver_profile_id, event_type, payload)
      VALUES (:tripId, :driverProfileId, :eventType, CAST(:payload AS jsonb))
      """,
      nativeQuery = true)
  int insertLocationEvent(
      @Param("tripId") Long tripId,
      @Param("driverProfileId") Long driverProfileId,
      @Param("eventType") String eventType,
      @Param("payload") String payload);

  default void save(LocationUpdateRequest request) {
    insertSample(
        request.tripId(),
        request.driverProfileId(),
        request.longitude(),
        request.latitude(),
        request.accuracyMeters(),
        request.speedMps(),
        request.bearingDegrees(),
        request.deviceRecordedAt());
  }

  interface PassengerLiveTripRow {
    Long getTripId();

    String getTripStatus();

    String getOriginLabel();

    String getDestinationLabel();

    Instant getDepartureTime();
  }

  interface AdminLiveTripRow {
    Long getTripId();

    Long getDriverProfileId();

    String getDriverName();

    String getTripStatus();

    String getOriginLabel();

    String getDestinationLabel();

    Instant getDepartureTime();
  }
}
