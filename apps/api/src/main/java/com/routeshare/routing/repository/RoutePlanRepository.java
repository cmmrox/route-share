package com.routeshare.routing.repository;

import com.routeshare.routing.entity.RoutePlanEntity;
import java.time.Instant;
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
}
