package com.routeshare.routing.repository;

import com.routeshare.routing.entity.PickupPointEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PickupPointRepository extends JpaRepository<PickupPointEntity, Long> {

  /**
   * Step one of the resolution chain: a curated landmark within reach.
   *
   * <p>Probed first on every resolve and costs nothing, so a corridor an operator has curated never
   * reaches Google at all.
   */
  @Query(
      value =
          """
      SELECT pickup_point_id AS "pickupPointId", label AS "label", description AS "description",
             side_hint AS "sideHint", source AS "source",
             ST_Y(position) AS "latitude", ST_X(position) AS "longitude",
             ST_Distance(position::geography,
                         ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) AS "metersAway"
        FROM routing.pickup_point
       WHERE active
         AND source = 'CURATED'
         AND ST_DWithin(position::geography,
                        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
       ORDER BY "metersAway" ASC
       LIMIT 1
      """,
      nativeQuery = true)
  Optional<PickupPointRow> findNearestCurated(
      @Param("lat") double lat, @Param("lng") double lng, @Param("radiusMeters") int radiusMeters);

  /**
   * Step two: a point already resolved at this corner by somebody else.
   *
   * <p>This is the row that makes the feature affordable. Colombo corridors have a finite number of
   * sensible stopping places, so the hit rate climbs toward 100% as the library fills and Places
   * usage trends to zero.
   */
  @Query(
      value =
          """
      SELECT pickup_point_id AS "pickupPointId", label AS "label", description AS "description",
             side_hint AS "sideHint", source AS "source",
             ST_Y(position) AS "latitude", ST_X(position) AS "longitude",
             ST_Distance(position::geography,
                         ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) AS "metersAway"
        FROM routing.pickup_point
       WHERE active
         AND source <> 'CURATED'
         AND ST_DWithin(position::geography,
                        ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
       ORDER BY "metersAway" ASC
       LIMIT 1
      """,
      nativeQuery = true)
  Optional<PickupPointRow> findNearestPersisted(
      @Param("lat") double lat, @Param("lng") double lng, @Param("radiusMeters") int radiusMeters);

  /**
   * Step three: a label somebody already paid Google for.
   *
   * <p>Every published route carries an {@code origin_label} and a {@code destination_label} that
   * were resolved through Places when the driver created it. Those names are bought and paid for —
   * checking them before calling anything is free.
   */
  @Query(
      value =
          """
      SELECT label FROM (
        SELECT r.origin_label AS label,
               ST_Distance(r.origin_point::geography,
                           ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) AS meters_away
          FROM routing.route_plan r
         WHERE ST_DWithin(r.origin_point::geography,
                          ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
        UNION ALL
        SELECT r.destination_label AS label,
               ST_Distance(ST_EndPoint(r.route_line)::geography,
                           ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) AS meters_away
          FROM routing.route_plan r
         WHERE ST_DWithin(ST_EndPoint(r.route_line)::geography,
                          ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
      ) AS labels
       ORDER BY meters_away ASC
       LIMIT 1
      """,
      nativeQuery = true)
  Optional<String> findNearestRouteLabel(
      @Param("lat") double lat, @Param("lng") double lng, @Param("radiusMeters") int radiusMeters);

  @Query(
      value =
          """
      INSERT INTO routing.pickup_point(label, description, side_hint, position, source,
                                       google_place_id, created_by_app_user_id)
      VALUES (:label, :description, :sideHint, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), :source,
              :googlePlaceId, :createdBy)
      RETURNING pickup_point_id
      """,
      nativeQuery = true)
  long insertPoint(
      @Param("label") String label,
      @Param("description") String description,
      @Param("sideHint") String sideHint,
      @Param("lat") double lat,
      @Param("lng") double lng,
      @Param("source") String source,
      @Param("googlePlaceId") String googlePlaceId,
      @Param("createdBy") Long createdBy);

  @Query(
      value =
          """
      SELECT pickup_point_id AS "pickupPointId", label AS "label", description AS "description",
             side_hint AS "sideHint", source AS "source",
             ST_Y(position) AS "latitude", ST_X(position) AS "longitude", 0.0 AS "metersAway"
        FROM routing.pickup_point WHERE pickup_point_id = :id
      """,
      nativeQuery = true)
  Optional<PickupPointRow> findRow(@Param("id") long id);

  /** What tier 3 will one day promote on. Incremented when a point is actually used. */
  @Modifying
  @Query(
      value =
          """
      UPDATE routing.pickup_point SET success_count = success_count + 1
       WHERE pickup_point_id = :id
      """,
      nativeQuery = true)
  int recordUse(@Param("id") long id);

  @Query(
      value =
          """
      SELECT pickup_point_id AS "pickupPointId", label AS "label", description AS "description",
             side_hint AS "sideHint", source AS "source",
             ST_Y(position) AS "latitude", ST_X(position) AS "longitude", 0.0 AS "metersAway"
        FROM routing.pickup_point
       WHERE (:source IS NULL OR source = :source)
       ORDER BY pickup_point_id DESC
       LIMIT 200
      """,
      nativeQuery = true)
  List<PickupPointRow> listBySource(@Param("source") String source);

  interface PickupPointRow {
    long getPickupPointId();

    String getLabel();

    String getDescription();

    String getSideHint();

    String getSource();

    double getLatitude();

    double getLongitude();

    double getMetersAway();
  }
}
