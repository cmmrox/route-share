package com.routeshare.routing.repository;

import com.routeshare.routing.entity.RouteOccurrenceEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
      RETURNING occurrence.route_plan_id AS "routePlanId", route.route_length_m AS "routeLengthMeters"
      """,
      nativeQuery = true)
  Optional<RouteReservationRow> reserveSeatsAndReturnRouteLength(
      @Param("routeOccurrenceId") long routeOccurrenceId, @Param("seats") int seats);

  interface RouteReservationRow {
    long getRoutePlanId();

    double getRouteLengthMeters();
  }
}
