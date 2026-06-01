package com.routeshare.location.repository;

import com.routeshare.location.dto.request.LocationUpdateRequest;
import com.routeshare.location.entity.LocationSampleEntity;
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
      @Param("tripId") long tripId,
      @Param("driverProfileId") long driverProfileId,
      @Param("lng") Double longitude,
      @Param("lat") Double latitude,
      @Param("accuracyMeters") Double accuracyMeters,
      @Param("speedMps") Double speedMps,
      @Param("bearingDegrees") Double bearingDegrees,
      @Param("deviceRecordedAt") java.time.Instant deviceRecordedAt);

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
}
