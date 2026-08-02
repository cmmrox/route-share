package com.routeshare.routing.repository;

import com.routeshare.routing.entity.RoutePlanEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

  /**
   * P04's list, and the count of what the radius removed, in one statement.
   *
   * <p>The predicate changed in slice 09 and this is the whole of it: a candidate is kept when the
   * driver's <b>trip origin</b> is within the radius of the rider's pickup, not when his route line
   * happens to pass near it. Pickup proximity survives as a scoring input below; it is no longer
   * what filters.
   *
   * <p>{@code filteredOutByRadius} is a windowed count over the same candidate set rather than a
   * second query, because two round trips against a moving table eventually disagree — and P04
   * shows both numbers side by side, so a disagreement is visible to the rider.
   *
   * <p>Eligibility is applied <em>inside</em> the query rather than as a post-filter, or the page
   * counts would describe a list the rider is not being shown.
   */
  @Query(
      value =
          """
      WITH request_points AS (
        SELECT
          ST_SetSRID(ST_MakePoint(:pickupLng, :pickupLat), 4326) AS pickup,
          ST_SetSRID(ST_MakePoint(:dropoffLng, :dropoffLat), 4326) AS dropoff
      ), corridor AS (
        SELECT
          r.route_plan_id AS "routePlanId",
          o.route_occurrence_id AS "routeOccurrenceId",
          r.origin_label AS "originLabel",
          r.destination_label AS "destinationLabel",
          o.scheduled_departure_at AS "departureTime",
          o.available_seats AS "availableSeats",
          r.route_length_m AS "routeLengthMeters",
          o.approval_mode AS "approvalMode",
          o.gender_policy AS "genderPolicy",
          o.verified_riders_only AS "verifiedRidersOnly",
          ST_LineLocatePoint(r.route_line, p.pickup) AS "pickupFraction",
          ST_LineLocatePoint(r.route_line, p.dropoff) AS "dropoffFraction",
          ST_Distance(r.route_line::geography, p.pickup::geography) AS "pickupDistanceMeters",
          ST_Distance(r.route_line::geography, p.dropoff::geography) AS "dropoffDistanceMeters",
          -- The number the filter turns on, projected as well as tested: P04 prints it on every
          -- card, and computing it twice is how the card and the list disagree.
          ST_Distance(r.origin_point::geography, p.pickup::geography) AS "startsMetersAway",
          COALESCE(dp.display_name, dau.display_name, 'Driver') AS "driverName",
          r.vehicle_id AS "vehicleId",
          v.make AS "vehicleMake",
          v.model AS "vehicleModel",
          v.color AS "vehicleColour",
          v.registration_number AS "vehicleRegistration",
          v.seat_count AS "vehicleSeatCount",
          v.class_key AS "vehicleClassKey",
          band.chosen_rate AS "ratePerKm",
          band.rate_min AS "classBandMin",
          band.rate_max AS "classBandMax",
          p.pickup AS pickup,
          p.dropoff AS dropoff,
          r.route_line AS "routeLine"
        FROM routing.route_occurrence o
        JOIN routing.route_plan r ON r.route_plan_id = o.route_plan_id
        JOIN driver.driver_profile dp ON dp.driver_profile_id = r.driver_profile_id
        LEFT JOIN identity.app_user dau ON dau.app_user_id = dp.app_user_id
        LEFT JOIN vehicle.vehicle v ON v.vehicle_id = r.vehicle_id
        LEFT JOIN vehicle.vehicle_rate_band band ON band.vehicle_id = r.vehicle_id
        CROSS JOIN request_points p
        WHERE r.status = 'PUBLISHED'
          AND o.status = 'PUBLISHED'
          AND o.scheduled_departure_at BETWEEN :windowStart AND :windowEnd
          AND o.available_seats >= :seats
          AND EXISTS (
            SELECT 1
            FROM routing.route_bucket_cell b
            WHERE b.route_occurrence_id = o.route_occurrence_id
              AND b.bucket_resolution = :bucketResolution
              AND b.bucket_cell IN (:pickupBucketCell, :dropoffBucketCell)
          )
          -- Slice 08, applied here rather than afterwards so the counts below describe the list
          -- the rider actually sees.
          AND (o.gender_policy = 'ANYONE' OR CAST(:riderVerifiedFemale AS BOOLEAN))
          AND (o.verified_riders_only = false OR CAST(:riderVerified AS BOOLEAN))
      ), scored AS (
        SELECT
          corridor.*,
          ST_Length(ST_LineSubstring("routeLine", "pickupFraction", "dropoffFraction")::geography)
            AS "overlapDistanceMeters",
          GREATEST(ST_Distance(pickup::geography, dropoff::geography), 1.0)
            AS "requestedDistanceMeters"
        FROM corridor
        WHERE "pickupFraction" < "dropoffFraction"
      ), counted AS (
        SELECT
          scored.*,
          ("startsMetersAway" <= :radiusMeters) AS "withinRadius",
          COUNT(*) OVER () AS "totalMatching",
          COUNT(*) FILTER (WHERE "startsMetersAway" > :radiusMeters) OVER () AS "filteredOut"
        FROM scored
      )
      SELECT
        "routePlanId", "routeOccurrenceId", "originLabel", "destinationLabel", "departureTime",
        "availableSeats", "routeLengthMeters", "approvalMode", "genderPolicy", "verifiedRidersOnly",
        "pickupFraction", "dropoffFraction", "pickupDistanceMeters", "dropoffDistanceMeters",
        "startsMetersAway", "driverName", "vehicleId", "vehicleMake", "vehicleModel",
        "vehicleColour", "vehicleRegistration", "vehicleSeatCount", "vehicleClassKey",
        "ratePerKm", "classBandMin", "classBandMax",
        "overlapDistanceMeters", "requestedDistanceMeters", "totalMatching", "filteredOut"
      FROM counted
      WHERE "withinRadius"
      -- Every ordering ends on the occurrence id. Without a total order two pages of one search can
      -- repeat a trip and drop another, and a rider who scrolled past a seat cannot find it again.
      ORDER BY
        CASE WHEN :sort = 'SOONEST' THEN "departureTime" END ASC,
        CASE WHEN :sort = 'CHEAPEST' THEN "ratePerKm" END ASC NULLS LAST,
        CASE WHEN :sort = 'BEST_MATCH'
             THEN "overlapDistanceMeters" / "requestedDistanceMeters" END DESC,
        "routeOccurrenceId" ASC
      OFFSET :offset LIMIT :limit
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
      @Param("radiusMeters") int radiusMeters,
      @Param("bucketResolution") int bucketResolution,
      @Param("pickupBucketCell") String pickupBucketCell,
      @Param("dropoffBucketCell") String dropoffBucketCell,
      @Param("sort") String sort,
      @Param("offset") int offset,
      @Param("limit") int limit,
      @Param("riderVerified") boolean riderVerified,
      @Param("riderVerifiedFemale") boolean riderVerifiedFemale);

  /**
   * The trips this rider's search just dropped on eligibility, and which rule dropped each one.
   *
   * <p>Slice 08. Without this there is nothing to count: a rider filtered out of search never makes
   * a request, so D35's "verified riders only cost you 3 requests last week" has no other source.
   *
   * <p>Deliberately narrow — the same corridor and window predicates, inverted on eligibility only,
   * returning ids rather than rows. The radius is not applied: a trip she could not have booked
   * anyway was not turned away by her driver's setting.
   */
  @Query(
      value =
          """
      SELECT o.route_occurrence_id AS "routeOccurrenceId",
             CASE WHEN o.gender_policy <> 'ANYONE' AND NOT CAST(:riderVerifiedFemale AS BOOLEAN)
                  THEN 'NOT_ELIGIBLE_WOMEN_ONLY'
                  ELSE 'NOT_ELIGIBLE_VERIFIED_ONLY'
             END AS "reason"
        FROM routing.route_occurrence o
        JOIN routing.route_plan r ON r.route_plan_id = o.route_plan_id
       WHERE r.status = 'PUBLISHED'
         AND o.status = 'PUBLISHED'
         AND o.scheduled_departure_at BETWEEN :windowStart AND :windowEnd
         AND o.available_seats >= :seats
         AND ST_DWithin(r.origin_point::geography,
                        ST_SetSRID(ST_MakePoint(:pickupLng, :pickupLat), 4326)::geography,
                        :radiusMeters)
         AND EXISTS (
           SELECT 1
             FROM routing.route_bucket_cell b
            WHERE b.route_occurrence_id = o.route_occurrence_id
              AND b.bucket_resolution = :bucketResolution
              AND b.bucket_cell IN (:pickupBucketCell, :dropoffBucketCell)
         )
         AND NOT ((o.gender_policy = 'ANYONE' OR CAST(:riderVerifiedFemale AS BOOLEAN))
                  AND (o.verified_riders_only = false OR CAST(:riderVerified AS BOOLEAN)))
       LIMIT :limit
      """,
      nativeQuery = true)
  List<EligibilityExclusionRow> findEligibilityExclusions(
      @Param("pickupLng") double pickupLng,
      @Param("pickupLat") double pickupLat,
      @Param("windowStart") Instant windowStart,
      @Param("windowEnd") Instant windowEnd,
      @Param("seats") int seats,
      @Param("radiusMeters") int radiusMeters,
      @Param("bucketResolution") int bucketResolution,
      @Param("pickupBucketCell") String pickupBucketCell,
      @Param("dropoffBucketCell") String dropoffBucketCell,
      @Param("limit") int limit,
      @Param("riderVerified") boolean riderVerified,
      @Param("riderVerifiedFemale") boolean riderVerifiedFemale);

  interface EligibilityExclusionRow {
    long getRouteOccurrenceId();

    String getReason();
  }

  @Query(
      value =
          """
      SELECT
        ST_AsGeoJSON(ST_LineSubstring(r.route_line, LEAST(:fromFraction, :toFraction), GREATEST(:fromFraction, :toFraction))) AS "geoJson",
        ST_Length(ST_LineSubstring(r.route_line, LEAST(:fromFraction, :toFraction), GREATEST(:fromFraction, :toFraction))::geography) AS "lengthMeters"
      FROM routing.route_occurrence o
      JOIN routing.route_plan r ON r.route_plan_id = o.route_plan_id
      WHERE o.route_occurrence_id = :routeOccurrenceId
        AND r.status = 'PUBLISHED'
      """,
      nativeQuery = true)
  Optional<RouteSegmentRow> findOccurrenceSegment(
      @Param("routeOccurrenceId") long routeOccurrenceId,
      @Param("fromFraction") double fromFraction,
      @Param("toFraction") double toFraction);

  interface RouteSegmentRow {
    String getGeoJson();

    Double getLengthMeters();
  }

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

  @Query(
      value =
          """
      SELECT
        r.route_plan_id AS "routePlanId",
        o.route_occurrence_id AS "routeOccurrenceId",
        r.vehicle_id AS "vehicleId",
        r.origin_label AS "originLabel",
        r.destination_label AS "destinationLabel",
        o.scheduled_departure_at AS "departureTime",
        o.available_seats AS "availableSeats",
        r.route_length_m AS "routeLengthMeters",
        r.status AS "routeStatus",
        o.status AS "occurrenceStatus"
      FROM routing.route_plan r
      LEFT JOIN routing.route_occurrence o ON o.route_plan_id = r.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      WHERE d.app_user_id = :driverAppUserId
      ORDER BY COALESCE(o.scheduled_departure_at, r.departure_time) DESC
      """,
      nativeQuery = true)
  List<DriverRouteRow> findDriverRoutes(@Param("driverAppUserId") long driverAppUserId);

  @Query(
      value =
          """
      SELECT
        r.route_plan_id AS "routePlanId",
        o.route_occurrence_id AS "routeOccurrenceId",
        r.vehicle_id AS "vehicleId",
        r.origin_label AS "originLabel",
        r.destination_label AS "destinationLabel",
        o.scheduled_departure_at AS "departureTime",
        o.available_seats AS "availableSeats",
        r.route_length_m AS "routeLengthMeters",
        r.status AS "routeStatus",
        o.status AS "occurrenceStatus"
      FROM routing.route_plan r
      LEFT JOIN routing.route_occurrence o ON o.route_plan_id = r.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      WHERE d.app_user_id = :driverAppUserId
        AND r.route_plan_id = :routePlanId
      """,
      nativeQuery = true)
  Optional<DriverRouteRow> findDriverRoute(
      @Param("driverAppUserId") long driverAppUserId, @Param("routePlanId") long routePlanId);

  @Modifying
  @Query(
      value =
          """
      UPDATE routing.route_plan r
      SET status = 'CANCELLED'
      FROM driver.driver_profile d
      WHERE r.driver_profile_id = d.driver_profile_id
        AND d.app_user_id = :driverAppUserId
        AND r.route_plan_id = :routePlanId
        AND r.status IN ('DRAFT','PUBLISHED')
      """,
      nativeQuery = true)
  int cancelDriverRoutePlan(
      @Param("driverAppUserId") long driverAppUserId, @Param("routePlanId") long routePlanId);

  @Modifying
  @Query(
      value =
          """
      UPDATE routing.route_occurrence o
      SET status = 'CANCELLED'
      WHERE o.route_plan_id = :routePlanId
        AND o.status = 'PUBLISHED'
      """,
      nativeQuery = true)
  int cancelRouteOccurrences(@Param("routePlanId") long routePlanId);

  @Query(
      value =
          """
      SELECT EXISTS(
        SELECT 1
        FROM routing.route_plan r
        JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
        WHERE r.route_plan_id = :routePlanId
          AND d.app_user_id = :driverAppUserId
          AND r.status = 'PUBLISHED'
      )
      """,
      nativeQuery = true)
  boolean isPublishedDriverRoute(
      @Param("driverAppUserId") long driverAppUserId, @Param("routePlanId") long routePlanId);

  @Query(
      value =
          """
      INSERT INTO routing.route_share_link(route_plan_id, share_token, share_url, qr_payload)
      VALUES (:routePlanId, :shareToken, :shareUrl, :qrPayload)
      ON CONFLICT (route_plan_id) DO UPDATE SET share_url = EXCLUDED.share_url
      RETURNING share_url
      """,
      nativeQuery = true)
  String upsertShareLink(
      @Param("routePlanId") long routePlanId,
      @Param("shareToken") String shareToken,
      @Param("shareUrl") String shareUrl,
      @Param("qrPayload") String qrPayload);

  interface DriverRouteRow {
    Long getRoutePlanId();

    Long getRouteOccurrenceId();

    Long getVehicleId();

    String getOriginLabel();

    String getDestinationLabel();

    Instant getDepartureTime();

    Integer getAvailableSeats();

    BigDecimal getRouteLengthMeters();

    String getRouteStatus();

    String getOccurrenceStatus();
  }

  interface RouteSearchCandidateRow {
    long getRoutePlanId();

    long getRouteOccurrenceId();

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

    String getDriverName();

    /** The vehicle whose assessed rate band prices this candidate. */
    Long getVehicleId();

    String getVehicleMake();

    String getVehicleModel();

    String getVehicleRegistration();

    Integer getVehicleSeatCount();

    // ── slice 09: the fields P04 prints without doing arithmetic ─────────────────────────────────

    /** The distance the radius filtered on, projected so the card and the filter cannot differ. */
    double getStartsMetersAway();

    String getVehicleColour();

    String getVehicleClassKey();

    /** The driver's chosen point inside his class band — P07 explains his rate from these three. */
    BigDecimal getRatePerKm();

    BigDecimal getClassBandMin();

    BigDecimal getClassBandMax();

    String getApprovalMode();

    String getGenderPolicy();

    boolean getVerifiedRidersOnly();

    /** Candidates on this corridor before the radius was applied — the same for every row. */
    long getTotalMatching();

    /** How many the radius removed. P04's card. */
    long getFilteredOut();
  }
}
