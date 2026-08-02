package com.routeshare.passenger.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * P02's saved commute — a stored search, not a new domain.
 *
 * <p>The geometry stays in SQL rather than becoming a mapped entity: nothing outside this table
 * needs a PostGIS type, and every read here projects plain latitude and longitude.
 */
public interface UsualCommuteRepository
    extends JpaRepository<com.routeshare.passenger.entity.UsualCommuteEntity, Long> {

  @Query(
      value =
          """
      SELECT origin_label AS "originLabel", ST_Y(origin) AS "originLatitude",
             ST_X(origin) AS "originLongitude", destination_label AS "destinationLabel",
             ST_Y(destination) AS "destinationLatitude",
             ST_X(destination) AS "destinationLongitude",
             habitual_time AS "habitualTime"
        FROM passenger.usual_commute WHERE app_user_id = :appUserId
      """,
      nativeQuery = true)
  Optional<UsualCommuteRow> findByAppUserId(@Param("appUserId") long appUserId);

  @Transactional
  @Modifying
  @Query(
      value =
          """
      INSERT INTO passenger.usual_commute(app_user_id, origin_label, origin, destination_label,
                                          destination, habitual_time)
      VALUES (:appUserId, :originLabel, ST_SetSRID(ST_MakePoint(:originLng, :originLat), 4326),
              :destinationLabel,
              ST_SetSRID(ST_MakePoint(:destinationLng, :destinationLat), 4326),
              CAST(:habitualTime AS TIME))
      ON CONFLICT (app_user_id) DO UPDATE
        SET origin_label = EXCLUDED.origin_label, origin = EXCLUDED.origin,
            destination_label = EXCLUDED.destination_label, destination = EXCLUDED.destination,
            habitual_time = EXCLUDED.habitual_time, updated_at = now()
      """,
      nativeQuery = true)
  void upsert(
      @Param("appUserId") long appUserId,
      @Param("originLabel") String originLabel,
      @Param("originLat") double originLat,
      @Param("originLng") double originLng,
      @Param("destinationLabel") String destinationLabel,
      @Param("destinationLat") double destinationLat,
      @Param("destinationLng") double destinationLng,
      @Param("habitualTime") String habitualTime);

  @Transactional
  @Modifying
  @Query(
      value = "DELETE FROM passenger.usual_commute WHERE app_user_id = :appUserId",
      nativeQuery = true)
  void clear(@Param("appUserId") long appUserId);

  interface UsualCommuteRow {
    String getOriginLabel();

    double getOriginLatitude();

    double getOriginLongitude();

    String getDestinationLabel();

    double getDestinationLatitude();

    double getDestinationLongitude();

    java.sql.Time getHabitualTime();
  }
}
