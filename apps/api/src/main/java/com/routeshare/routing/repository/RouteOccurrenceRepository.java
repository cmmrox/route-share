package com.routeshare.routing.repository;

import com.routeshare.routing.entity.RouteOccurrenceEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteOccurrenceRepository extends JpaRepository<RouteOccurrenceEntity, Long> {
  @Query(
      value =
          """
      INSERT INTO routing.route_occurrence(route_plan_id, scheduled_departure_at, available_seats)
      VALUES (:routePlanId, :departureAt, :availableSeats)
      RETURNING route_occurrence_id
      """,
      nativeQuery = true)
  long insertOccurrence(
      @Param("routePlanId") long routePlanId,
      @Param("departureAt") Instant departureAt,
      @Param("availableSeats") int availableSeats);

  @Query(
      value =
          """
      UPDATE routing.route_occurrence occurrence
      SET available_seats = occurrence.available_seats - :seats
      FROM routing.route_plan route
      WHERE occurrence.route_plan_id = route.route_plan_id
        AND occurrence.route_occurrence_id = :routeOccurrenceId
        AND occurrence.status = 'PUBLISHED'
        AND occurrence.scheduled_departure_at > now()
        AND occurrence.available_seats >= :seats
      RETURNING occurrence.route_plan_id AS "routePlanId", route.vehicle_id AS "vehicleId",
                route.route_length_m AS "routeLengthMeters"
      """,
      nativeQuery = true)
  Optional<RouteReservationRow> reserveSeatsAndReturnRouteLength(
      @Param("routeOccurrenceId") long routeOccurrenceId, @Param("seats") int seats);

  /**
   * Returns seats to inventory when a booking stops holding them — a no-show release, today.
   *
   * <p>Bounded by the plan's own capacity so a double release can never inflate a car beyond the
   * seats it has. Offering the freed seat to somebody else is slice 07's problem; this only makes
   * it available again.
   */
  @Modifying
  @Query(
      value =
          """
      UPDATE routing.route_occurrence occurrence
      SET status = 'CANCELLED'
      FROM routing.route_plan route
      WHERE occurrence.route_plan_id = route.route_plan_id
        AND route.driver_profile_id = :driverProfileId
        AND occurrence.status = 'PUBLISHED'
        AND occurrence.scheduled_departure_at > now()
      """,
      nativeQuery = true)
  int cancelFutureForDriver(@Param("driverProfileId") long driverProfileId);

  @Modifying
  @Query(
      value =
          """
      UPDATE routing.route_occurrence occurrence
      SET available_seats = LEAST(occurrence.available_seats + :seats, route.available_seats)
      FROM routing.route_plan route
      WHERE occurrence.route_plan_id = route.route_plan_id
        AND occurrence.route_occurrence_id = :routeOccurrenceId
      """,
      nativeQuery = true)
  int releaseSeats(@Param("routeOccurrenceId") long routeOccurrenceId, @Param("seats") int seats);

  @Query(
      value =
          """
      SELECT route.vehicle_id AS "vehicleId", route.route_length_m AS "routeLengthMeters"
      FROM routing.route_occurrence occurrence
      JOIN routing.route_plan route ON route.route_plan_id = occurrence.route_plan_id
      WHERE occurrence.route_occurrence_id = :routeOccurrenceId
      """,
      nativeQuery = true)
  Optional<PriceableTripRow> findPriceableTrip(@Param("routeOccurrenceId") long routeOccurrenceId);

  interface PriceableTripRow {
    long getVehicleId();

    double getRouteLengthMeters();
  }

  interface RouteReservationRow {
    long getRoutePlanId();

    /** The vehicle whose rate band prices the booking being made. */
    long getVehicleId();

    double getRouteLengthMeters();
  }

  @Query(
      value =
          """
      SELECT route_occurrence_id AS "occurrenceId", scheduled_departure_at AS "scheduledAt",
             available_seats AS "availableSeats"
      FROM routing.route_occurrence
      WHERE route_plan_id = :routePlanId
      ORDER BY scheduled_departure_at DESC
      LIMIT 1
      """,
      nativeQuery = true)
  Optional<LatestOccurrenceRow> findLatestForPlan(@Param("routePlanId") long routePlanId);

  interface LatestOccurrenceRow {
    long getOccurrenceId();

    Instant getScheduledAt();

    int getAvailableSeats();
  }

  // ── slice 07: approval mode, cancellation and alternatives ───────────────────────────────────

  @Modifying
  @Query(
      value =
          """
      UPDATE routing.route_occurrence
         SET approval_mode = :mode
       WHERE route_occurrence_id = :routeOccurrenceId
      """,
      nativeQuery = true)
  int updateApprovalMode(
      @Param("routeOccurrenceId") long routeOccurrenceId, @Param("mode") String mode);

  @Query(
      value =
          """
      SELECT EXISTS(
        SELECT 1
          FROM routing.route_occurrence o
          JOIN routing.route_plan p ON p.route_plan_id = o.route_plan_id
          JOIN driver.driver_profile d ON d.driver_profile_id = p.driver_profile_id
         WHERE o.route_occurrence_id = :routeOccurrenceId
           AND d.app_user_id = :appUserId)
      """,
      nativeQuery = true)
  boolean isOwnedByDriverAppUser(
      @Param("routeOccurrenceId") long routeOccurrenceId, @Param("appUserId") long appUserId);

  @Modifying
  @Query(
      value =
          """
      UPDATE routing.route_occurrence
         SET status = 'CANCELLED'
       WHERE route_occurrence_id = :routeOccurrenceId
         AND status = 'PUBLISHED'
      """,
      nativeQuery = true)
  int cancelOccurrence(@Param("routeOccurrenceId") long routeOccurrenceId);

  /**
   * The riders a cancellation strands, by first name only. D31 names them to the driver who owes
   * them a share of his fee; a surname or a number would make a fee notice a disclosure.
   *
   * <p>A phone-OTP account carries its number as its display name, so anything that is not plainly
   * a name falls back to a generic label rather than printing a contact detail.
   */
  @Query(
      value =
          """
      SELECT b.booking_id AS "bookingId",
             b.passenger_app_user_id AS "passengerAppUserId",
             CASE
               WHEN u.display_name IS NULL THEN 'A rider'
               WHEN u.display_name ~ '[0-9@]' THEN 'A rider'
               ELSE split_part(btrim(u.display_name), ' ', 1)
             END AS "firstName"
        FROM booking.booking b
        JOIN identity.app_user u ON u.app_user_id = b.passenger_app_user_id
       WHERE b.route_occurrence_id = :routeOccurrenceId
         AND b.status IN ('REQUESTED', 'CONFIRMED')
       ORDER BY b.booking_id
      """,
      nativeQuery = true)
  java.util.List<AffectedRiderRow> findAffectedRiders(
      @Param("routeOccurrenceId") long routeOccurrenceId);

  interface AffectedRiderRow {
    Long getBookingId();

    Long getPassengerAppUserId();

    String getFirstName();
  }

  /**
   * Other trips on the same corridor, near the same time (P13, P22, P24).
   *
   * <p>Ranked by how much of the original route they share and then by rate, which is the order the
   * screens present. Bounded to a day either side because a trip tomorrow is not an alternative to
   * one this evening, however well it matches.
   */
  @Query(
      value =
          """
      SELECT o.route_occurrence_id AS "routeOccurrenceId",
             CASE
               WHEN u.display_name IS NULL THEN 'A driver'
               WHEN u.display_name ~ '[0-9@]' THEN 'A driver'
               ELSE split_part(btrim(u.display_name), ' ', 1)
             END AS "driverFirstName",
             p.origin_label AS "originLabel",
             p.destination_label AS "destinationLabel",
             o.scheduled_departure_at AS "departsAt",
             o.available_seats AS "seatsAvailable",
             band.chosen_rate AS "ratePerKm",
             ROUND((ST_Length(ST_Intersection(
                      ST_Buffer(origin.route_line::geography, 400)::geometry,
                      p.route_line)::geography)
                    / NULLIF(ST_Length(origin.route_line::geography), 0) * 100)::numeric, 2)
               AS "matchPercent"
        FROM routing.route_occurrence origin_occurrence
        JOIN routing.route_plan origin ON origin.route_plan_id = origin_occurrence.route_plan_id
        JOIN routing.route_occurrence o ON o.route_occurrence_id <> origin_occurrence.route_occurrence_id
        JOIN routing.route_plan p ON p.route_plan_id = o.route_plan_id
        LEFT JOIN vehicle.vehicle_rate_band band ON band.vehicle_id = p.vehicle_id
        JOIN driver.driver_profile d ON d.driver_profile_id = p.driver_profile_id
        JOIN identity.app_user u ON u.app_user_id = d.app_user_id
       WHERE origin_occurrence.route_occurrence_id = :routeOccurrenceId
         AND o.status = 'PUBLISHED'
         AND o.available_seats > 0
         AND o.scheduled_departure_at > now()
         AND o.scheduled_departure_at
               BETWEEN origin_occurrence.scheduled_departure_at - interval '6 hours'
                   AND origin_occurrence.scheduled_departure_at + interval '6 hours'
         AND ST_DWithin(origin.route_line::geography, p.route_line::geography, 2000)
       ORDER BY "matchPercent" DESC NULLS LAST, band.chosen_rate ASC NULLS LAST
       LIMIT 5
      """,
      nativeQuery = true)
  java.util.List<AlternativeRow> findAlternatives(
      @Param("routeOccurrenceId") long routeOccurrenceId);

  interface AlternativeRow {
    Long getRouteOccurrenceId();

    String getDriverFirstName();

    String getOriginLabel();

    String getDestinationLabel();

    Instant getDepartsAt();

    Integer getSeatsAvailable();

    java.math.BigDecimal getRatePerKm();

    java.math.BigDecimal getMatchPercent();
  }
}
