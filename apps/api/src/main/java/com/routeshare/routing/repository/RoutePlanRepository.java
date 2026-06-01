package com.routeshare.routing.repository;

import com.routeshare.routing.entity.RoutePlanEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutePlanRepository extends JpaRepository<RoutePlanEntity, Long> {
  @Query(
      value =
          """
      INSERT INTO routing.route_plan(
        driver_profile_id, vehicle_id, origin_label, destination_label, route_line,
        route_length_m, departure_time, available_seats)
      VALUES (:driverProfileId, :vehicleId, :originLabel, :destinationLabel,
        ST_GeomFromText('LINESTRING(' || :points || ')', 4326),
        ST_Length(ST_GeomFromText('LINESTRING(' || :points || ')', 4326)::geography),
        :departureTime, :availableSeats)
      RETURNING route_plan_id
      """,
      nativeQuery = true)
  long insertReturningId(
      @Param("driverProfileId") long driverProfileId,
      @Param("vehicleId") long vehicleId,
      @Param("originLabel") String originLabel,
      @Param("destinationLabel") String destinationLabel,
      @Param("points") String lineStringPoints,
      @Param("departureTime") Instant departureTime,
      @Param("availableSeats") int availableSeats);

  @Query(
      value =
          """
      UPDATE routing.route_plan
      SET available_seats = available_seats - :seats
      WHERE route_plan_id = :routePlanId
        AND status = 'PUBLISHED'
        AND departure_time > now()
        AND available_seats >= :seats
      RETURNING route_length_m
      """,
      nativeQuery = true)
  Optional<Double> reserveSeatsAndReturnRouteLength(
      @Param("routePlanId") long routePlanId, @Param("seats") int seats);

  @Query(
      value =
          """
      WITH request_points AS (
        SELECT
          ST_SetSRID(ST_MakePoint(:pickupLng, :pickupLat), 4326) AS pickup,
          ST_SetSRID(ST_MakePoint(:dropoffLng, :dropoffLat), 4326) AS dropoff
      ), candidate_routes AS (
        SELECT
          r.route_plan_id AS "routePlanId",
          r.origin_label AS "originLabel",
          r.destination_label AS "destinationLabel",
          r.departure_time AS "departureTime",
          r.available_seats AS "availableSeats",
          r.route_length_m AS "routeLengthMeters",
          ST_LineLocatePoint(r.route_line, p.pickup) AS "pickupFraction",
          ST_LineLocatePoint(r.route_line, p.dropoff) AS "dropoffFraction",
          ST_Distance(r.route_line::geography, p.pickup::geography) AS "pickupDistanceMeters",
          ST_Distance(r.route_line::geography, p.dropoff::geography) AS "dropoffDistanceMeters",
          p.pickup AS pickup,
          p.dropoff AS dropoff,
          r.route_line AS "routeLine"
        FROM routing.route_plan r
        CROSS JOIN request_points p
        WHERE r.status = 'PUBLISHED'
          AND r.departure_time BETWEEN :windowStart AND :windowEnd
          AND r.available_seats >= :seats
          AND EXISTS (
            SELECT 1
            FROM routing.route_bucket_cell b
            WHERE b.route_plan_id = r.route_plan_id
              AND b.bucket_resolution = :bucketResolution
              AND b.bucket_cell IN (:pickupBucketCell, :dropoffBucketCell)
          )
          AND ST_DWithin(r.route_line::geography, p.pickup::geography, :pickupRadiusMeters)
          AND ST_DWithin(r.route_line::geography, p.dropoff::geography, :dropoffRadiusMeters)
      )
      SELECT
        "routePlanId",
        "originLabel",
        "destinationLabel",
        "departureTime",
        "availableSeats",
        "routeLengthMeters",
        "pickupFraction",
        "dropoffFraction",
        "pickupDistanceMeters",
        "dropoffDistanceMeters",
        ST_Length(ST_LineSubstring("routeLine", "pickupFraction", "dropoffFraction")::geography) AS "overlapDistanceMeters",
        GREATEST(ST_Distance(pickup::geography, dropoff::geography), 1.0) AS "requestedDistanceMeters"
      FROM candidate_routes
      WHERE "pickupFraction" < "dropoffFraction"
      ORDER BY "departureTime" ASC, "pickupDistanceMeters" ASC, "dropoffDistanceMeters" ASC
      LIMIT :limit
      """,
      nativeQuery = true)
  List<RouteSearchCandidateRow> findSearchCandidates(
      @Param("pickupLng") double pickupLng,
      @Param("pickupLat") double pickupLat,
      @Param("dropoffLng") double dropoffLng,
      @Param("dropoffLat") double dropoffLat,
      @Param("windowStart") Instant windowStart,
      @Param("windowEnd") Instant windowEnd,
      @Param("seats") int seats,
      @Param("pickupRadiusMeters") int pickupRadiusMeters,
      @Param("dropoffRadiusMeters") int dropoffRadiusMeters,
      @Param("bucketResolution") int bucketResolution,
      @Param("pickupBucketCell") String pickupBucketCell,
      @Param("dropoffBucketCell") String dropoffBucketCell,
      @Param("limit") int limit);

  default long create(
      long driverProfileId,
      long vehicleId,
      String originLabel,
      String destinationLabel,
      String lineStringPoints,
      Instant departureTime,
      int availableSeats) {
    return insertReturningId(
        driverProfileId,
        vehicleId,
        originLabel,
        destinationLabel,
        lineStringPoints,
        departureTime,
        availableSeats);
  }

  interface RouteSearchCandidateRow {
    long getRoutePlanId();

    String getOriginLabel();

    String getDestinationLabel();

    Instant getDepartureTime();

    int getAvailableSeats();

    double getRouteLengthMeters();

    double getPickupFraction();

    double getDropoffFraction();

    double getPickupDistanceMeters();

    double getDropoffDistanceMeters();

    double getOverlapDistanceMeters();

    double getRequestedDistanceMeters();
  }
}
