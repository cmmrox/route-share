package com.routeshare.location.repository;

import com.routeshare.location.entity.TripProgressEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface LocationPipelineRepository extends JpaRepository<TripProgressEntity, Long> {
  @Query(
      value =
          """
          SELECT d.driver_profile_id AS "driverProfileId", t.status AS "tripStatus"
            FROM trip.trip t
            JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
            JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
           WHERE t.trip_id = :tripId AND d.app_user_id = :appUserId
          """,
      nativeQuery = true)
  Optional<DriverTripAccessRow> driverTripAccess(
      @Param("tripId") long tripId, @Param("appUserId") long appUserId);

  @Query(
      value =
          """
          WITH route AS (
              SELECT r.route_line,
                     ST_Length(r.route_line::geography) AS total_m,
                     ST_SetSRID(ST_MakePoint(:lng, :lat), 4326) AS fix
                FROM trip.trip t
                JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
               WHERE t.trip_id = :tripId
          ), segments AS (
              SELECT d.path[1] AS segment_no, d.geom, route.fix, route.total_m,
                     ST_Length(d.geom::geography) AS segment_m
                FROM route, LATERAL ST_DumpSegments(route.route_line) d
          ), candidates AS (
              SELECT segment_no, geom, fix, total_m, segment_m,
                     COALESCE(
                         SUM(segment_m) OVER (
                             ORDER BY segment_no ROWS BETWEEN UNBOUNDED PRECEDING
                             AND 1 PRECEDING), 0) AS prior_m,
                     ST_LineLocatePoint(geom, fix) AS segment_fraction,
                     ST_Distance(geom::geography, fix::geography) AS offset_m
                FROM segments
          )
          SELECT LEAST(1.0, GREATEST(0.0,
                     (prior_m + segment_m * segment_fraction) / NULLIF(total_m, 0)))
                     AS "fraction",
                 offset_m AS "offsetMeters",
                 GREATEST(0.0, total_m - (prior_m + segment_m * segment_fraction))
                     AS "remainingMeters"
            FROM candidates
           ORDER BY offset_m, segment_no
           LIMIT 8
          """,
      nativeQuery = true)
  List<RouteProjectionRow> projectCandidates(
      @Param("tripId") long tripId, @Param("lat") double latitude, @Param("lng") double longitude);

  @Query(
      value =
          """
          SELECT p.trip_id AS "tripId", p.route_fraction AS "routeFraction",
                 p.confidence AS "confidence", p.matched_at AS "matchedAt",
                 p.updated_at AS "updatedAt", p.speed_mps AS "speedMps",
                 p.bearing_degrees AS "bearingDegrees",
                 ST_Y(p.last_position) AS "latitude", ST_X(p.last_position) AS "longitude",
                 p.off_route_since AS "offRouteSince",
                 p.reversal_candidate_fraction AS "reversalCandidateFraction",
                 p.reversal_candidate_count AS "reversalCandidateCount",
                 r.route_length_m AS "routeLengthMeters"
            FROM location.trip_progress p
            JOIN trip.trip t ON t.trip_id = p.trip_id
            JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
           WHERE p.trip_id = :tripId
          """,
      nativeQuery = true)
  Optional<ProgressRow> progress(@Param("tripId") long tripId);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO location.location_sample_dedupe(trip_id, sample_id)
          VALUES (:tripId, :sampleId)
          ON CONFLICT DO NOTHING
          """,
      nativeQuery = true)
  int claimSample(@Param("tripId") long tripId, @Param("sampleId") String sampleId);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO location.location_sample(
              trip_id, driver_profile_id, sample_id, point, accuracy_m, speed_mps,
              bearing_degrees, battery_pct, device_recorded_at, accepted, rejection_reason,
              route_fraction, route_offset_meters)
          VALUES (
              :tripId, :driverProfileId, :sampleId,
              ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), :accuracy, :speed, :bearing,
              :battery, :capturedAt, :accepted, :reason, :fraction, :offset)
          """,
      nativeQuery = true)
  int insertObservation(
      @Param("tripId") long tripId,
      @Param("driverProfileId") long driverProfileId,
      @Param("sampleId") String sampleId,
      @Param("lat") double latitude,
      @Param("lng") double longitude,
      @Param("accuracy") double accuracy,
      @Param("speed") Double speed,
      @Param("bearing") Double bearing,
      @Param("battery") Integer battery,
      @Param("capturedAt") Instant capturedAt,
      @Param("accepted") boolean accepted,
      @Param("reason") String rejectionReason,
      @Param("fraction") Double fraction,
      @Param("offset") Double offset);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO location.trip_progress(
              trip_id, route_fraction, confidence, matched_at, updated_at, speed_mps,
              bearing_degrees, last_position, off_route_since,
              reversal_candidate_fraction, reversal_candidate_count)
          VALUES (
              :tripId, :fraction, 'MATCHED', :matchedAt, :now, :speed, :bearing,
              ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), NULL, :candidate, :candidateCount)
          ON CONFLICT (trip_id) DO UPDATE SET
              route_fraction = EXCLUDED.route_fraction,
              confidence = 'MATCHED',
              matched_at = EXCLUDED.matched_at,
              updated_at = EXCLUDED.updated_at,
              speed_mps = EXCLUDED.speed_mps,
              bearing_degrees = EXCLUDED.bearing_degrees,
              last_position = EXCLUDED.last_position,
              off_route_since = NULL,
              reversal_candidate_fraction = EXCLUDED.reversal_candidate_fraction,
              reversal_candidate_count = EXCLUDED.reversal_candidate_count
          """,
      nativeQuery = true)
  int upsertMatched(
      @Param("tripId") long tripId,
      @Param("fraction") double fraction,
      @Param("matchedAt") Instant matchedAt,
      @Param("now") Instant now,
      @Param("speed") Double speed,
      @Param("bearing") Double bearing,
      @Param("lat") double latitude,
      @Param("lng") double longitude,
      @Param("candidate") Double candidate,
      @Param("candidateCount") int candidateCount);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          UPDATE location.trip_progress
             SET reversal_candidate_fraction = :candidate,
                 reversal_candidate_count = 1,
                 updated_at = :now
           WHERE trip_id = :tripId
          """,
      nativeQuery = true)
  int recordReversalCandidate(
      @Param("tripId") long tripId,
      @Param("candidate") double candidate,
      @Param("now") Instant now);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO location.trip_progress(
              trip_id, route_fraction, confidence, matched_at, updated_at, speed_mps,
              bearing_degrees, last_position, off_route_since)
          VALUES (
              :tripId, :fraction,
              CASE WHEN :graceSeconds = 0 THEN 'OFF_ROUTE' ELSE 'MATCHED' END,
              :capturedAt, :now, :speed, :bearing,
              ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), :now)
          ON CONFLICT (trip_id) DO UPDATE SET
              confidence = CASE
                  WHEN EXTRACT(EPOCH FROM (:now - COALESCE(location.trip_progress.off_route_since, :now)))
                       >= :graceSeconds THEN 'OFF_ROUTE'
                  ELSE location.trip_progress.confidence END,
              updated_at = :now,
              off_route_since = COALESCE(location.trip_progress.off_route_since, :now)
          """,
      nativeQuery = true)
  int recordOffRoute(
      @Param("tripId") long tripId,
      @Param("fraction") double fraction,
      @Param("capturedAt") Instant capturedAt,
      @Param("now") Instant now,
      @Param("speed") Double speed,
      @Param("bearing") Double bearing,
      @Param("lat") double latitude,
      @Param("lng") double longitude,
      @Param("graceSeconds") long graceSeconds);

  @Query(
      value =
          """
          SELECT p.trip_id AS "tripId", p.route_fraction AS "routeFraction",
                 p.confidence AS "confidence", ST_Y(p.last_position) AS "latitude",
                 ST_X(p.last_position) AS "longitude"
           FROM location.trip_progress p
           WHERE p.confidence IN ('MATCHED', 'EXTRAPOLATED')
             AND p.last_position && ST_Expand(
                 ST_SetSRID(ST_MakePoint(:lng, :lat), 4326),
                 :radiusMeters / 111320.0)
             AND ST_DWithin(
                 p.last_position::geography,
                 ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                 :radiusMeters)
           ORDER BY p.trip_id
          """,
      nativeQuery = true)
  List<CandidateRow> candidateTripsNear(
      @Param("lat") double latitude,
      @Param("lng") double longitude,
      @Param("radiusMeters") double radiusMeters);

  @Query(
      value =
          """
          SELECT EXISTS(
                     SELECT 1 FROM trip.trip t
                     JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
                     JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
                     WHERE d.app_user_id = :appUserId
                       AND t.status IN ('STARTED','ARRIVED_PICKUP','PASSENGER_ONBOARD'))
                     AS "running",
                 EXISTS(
                     SELECT 1 FROM routing.route_plan r
                     JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
                     WHERE d.app_user_id = :appUserId AND r.status = 'PUBLISHED')
                     AS "published",
                 EXISTS(
                     SELECT 1 FROM location.approach_session a
                     JOIN trip.trip t ON t.trip_id = a.trip_id
                     JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
                     JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
                     WHERE d.app_user_id = :appUserId AND a.closed_at IS NULL)
                     AS "approach"
          """,
      nativeQuery = true)
  PolicyStateRow policyState(@Param("appUserId") long appUserId);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          UPDATE location.trip_progress
             SET confidence = CASE
                 WHEN confidence = 'OFF_ROUTE' THEN 'OFF_ROUTE'
                 WHEN matched_at < :staleBefore THEN 'STALE'
                 WHEN matched_at < :now THEN 'EXTRAPOLATED'
                 ELSE 'MATCHED' END,
                 updated_at = :now
           WHERE confidence <> CASE
                 WHEN confidence = 'OFF_ROUTE' THEN 'OFF_ROUTE'
                 WHEN matched_at < :staleBefore THEN 'STALE'
                 WHEN matched_at < :now THEN 'EXTRAPOLATED'
                 ELSE 'MATCHED' END
          """,
      nativeQuery = true)
  int sweepConfidence(@Param("now") Instant now, @Param("staleBefore") Instant staleBefore);

  @Transactional
  @Modifying
  @Query(
      value = "DELETE FROM location.location_sample WHERE server_received_at < :cutoff",
      nativeQuery = true)
  int deleteSamplesBefore(@Param("cutoff") Instant cutoff);

  @Query(value = "SELECT location.ensure_location_sample_partitions()", nativeQuery = true)
  int ensurePartitions();

  interface RouteProjectionRow {
    Double getFraction();

    Double getOffsetMeters();

    Double getRemainingMeters();
  }

  interface DriverTripAccessRow {
    Long getDriverProfileId();

    String getTripStatus();
  }

  interface ProgressRow {
    Long getTripId();

    BigDecimal getRouteFraction();

    String getConfidence();

    Instant getMatchedAt();

    Instant getUpdatedAt();

    BigDecimal getSpeedMps();

    BigDecimal getBearingDegrees();

    Double getLatitude();

    Double getLongitude();

    Instant getOffRouteSince();

    BigDecimal getReversalCandidateFraction();

    Integer getReversalCandidateCount();

    BigDecimal getRouteLengthMeters();
  }

  interface CandidateRow {
    Long getTripId();

    BigDecimal getRouteFraction();

    String getConfidence();

    Double getLatitude();

    Double getLongitude();
  }

  interface PolicyStateRow {
    Boolean getRunning();

    Boolean getPublished();

    Boolean getApproach();
  }
}
