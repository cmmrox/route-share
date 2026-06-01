package com.routeshare.routing.repository;

import com.routeshare.routing.entity.RouteOccurrenceEntity;
import java.time.Instant;
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
}
