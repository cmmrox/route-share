package com.routeshare.trip.repository;

import com.routeshare.trip.domain.TripStatus;
import com.routeshare.trip.entity.TripEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<TripEntity, Long> {
  @Query(
      value =
          """
      SELECT EXISTS(
        SELECT 1 FROM trip.trip t
        JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
        JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
        WHERE t.trip_id = :tripId AND d.app_user_id = :appUserId)
      """,
      nativeQuery = true)
  boolean isOwnedByDriverAppUser(@Param("tripId") long tripId, @Param("appUserId") long appUserId);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select t.status from TripEntity t where t.id = :tripId")
  Optional<TripStatus> lockAndFindStatus(@Param("tripId") long tripId);

  @Query(
      value =
          """
      SELECT
        t.trip_id AS "tripId",
        t.route_plan_id AS "routePlanId",
        t.route_occurrence_id AS "routeOccurrenceId",
        r.origin_label AS "originLabel",
        r.destination_label AS "destinationLabel",
        ro.scheduled_departure_at AS "departureTime",
        t.status AS "status",
        COUNT(b.booking_id) FILTER (WHERE b.status = 'CONFIRMED') AS "confirmedBookings",
        COALESCE(SUM(b.seats) FILTER (WHERE b.status = 'CONFIRMED'), 0) AS "bookedSeats",
        t.started_at AS "startedAt",
        t.completed_at AS "completedAt"
      FROM trip.trip t
      JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
      LEFT JOIN routing.route_occurrence ro ON ro.route_occurrence_id = t.route_occurrence_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      LEFT JOIN booking.booking b ON b.route_plan_id = t.route_plan_id
        AND (b.route_occurrence_id = t.route_occurrence_id OR t.route_occurrence_id IS NULL)
      WHERE d.app_user_id = :driverAppUserId
      GROUP BY t.trip_id, t.route_plan_id, t.route_occurrence_id, r.origin_label, r.destination_label,
               ro.scheduled_departure_at, t.status, t.started_at, t.completed_at
      ORDER BY COALESCE(ro.scheduled_departure_at, t.started_at, t.completed_at) DESC
      """,
      nativeQuery = true)
  List<DriverTripRow> findDriverTrips(@Param("driverAppUserId") long driverAppUserId);

  @Query(
      value =
          """
      SELECT
        t.trip_id AS "tripId",
        t.route_plan_id AS "routePlanId",
        t.route_occurrence_id AS "routeOccurrenceId",
        r.origin_label AS "originLabel",
        r.destination_label AS "destinationLabel",
        ro.scheduled_departure_at AS "departureTime",
        t.status AS "status",
        COUNT(b.booking_id) FILTER (WHERE b.status = 'CONFIRMED') AS "confirmedBookings",
        COALESCE(SUM(b.seats) FILTER (WHERE b.status = 'CONFIRMED'), 0) AS "bookedSeats",
        t.started_at AS "startedAt",
        t.completed_at AS "completedAt"
      FROM trip.trip t
      JOIN routing.route_plan r ON r.route_plan_id = t.route_plan_id
      LEFT JOIN routing.route_occurrence ro ON ro.route_occurrence_id = t.route_occurrence_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      LEFT JOIN booking.booking b ON b.route_plan_id = t.route_plan_id
        AND (b.route_occurrence_id = t.route_occurrence_id OR t.route_occurrence_id IS NULL)
      WHERE d.app_user_id = :driverAppUserId
        AND t.trip_id = :tripId
      GROUP BY t.trip_id, t.route_plan_id, t.route_occurrence_id, r.origin_label, r.destination_label,
               ro.scheduled_departure_at, t.status, t.started_at, t.completed_at
      """,
      nativeQuery = true)
  Optional<DriverTripRow> findDriverTrip(
      @Param("driverAppUserId") long driverAppUserId, @Param("tripId") long tripId);

  @Modifying
  @Query(
      value =
          """
      INSERT INTO trip.pre_trip_checklist(
        trip_id, driver_app_user_id, vehicle_checked, documents_ready, route_reviewed, notes)
      VALUES (:tripId, :driverAppUserId, :vehicleChecked, :documentsReady, :routeReviewed, :notes)
      ON CONFLICT (trip_id) DO UPDATE SET
        vehicle_checked = EXCLUDED.vehicle_checked,
        documents_ready = EXCLUDED.documents_ready,
        route_reviewed = EXCLUDED.route_reviewed,
        notes = EXCLUDED.notes
      """,
      nativeQuery = true)
  int insertPreTripChecklist(
      @Param("tripId") long tripId,
      @Param("driverAppUserId") long driverAppUserId,
      @Param("vehicleChecked") boolean vehicleChecked,
      @Param("documentsReady") boolean documentsReady,
      @Param("routeReviewed") boolean routeReviewed,
      @Param("notes") String notes);

  @Modifying
  @Query(
      value =
          """
      INSERT INTO trip.trip_operational_event(trip_id, event_type, actor_app_user_id)
      VALUES (:tripId, 'ARRIVED_PICKUP', :actorAppUserId)
      ON CONFLICT (trip_id, event_type) DO NOTHING
      """,
      nativeQuery = true)
  int insertArrivedPickupEvent(
      @Param("tripId") long tripId, @Param("actorAppUserId") long actorAppUserId);

  /**
   * Materialises the trip for an occurrence. Reads the route plan from the occurrence rather than
   * taking it from the caller, so the two can never be paired wrongly.
   *
   * <p>{@code DO NOTHING} rather than a check-then-insert: the unique index is the only arbiter
   * that holds when two passengers book the last two seats in the same instant.
   */
  @Modifying
  @Query(
      value =
          """
      INSERT INTO trip.trip(route_plan_id, route_occurrence_id, status)
      SELECT ro.route_plan_id, ro.route_occurrence_id, 'SCHEDULED'
      FROM routing.route_occurrence ro
      WHERE ro.route_occurrence_id = :routeOccurrenceId
      ON CONFLICT (route_occurrence_id) WHERE route_occurrence_id IS NOT NULL DO NOTHING
      """,
      nativeQuery = true)
  int insertTripForOccurrence(@Param("routeOccurrenceId") long routeOccurrenceId);

  @Query("select t.id from TripEntity t where t.routeOccurrenceId = :routeOccurrenceId")
  Optional<Long> findTripIdByRouteOccurrenceId(@Param("routeOccurrenceId") long routeOccurrenceId);

  /** The only accepted source of a start window's departure time. */
  @Query(
      value =
          """
      SELECT ro.scheduled_departure_at
      FROM routing.route_occurrence ro
      WHERE ro.route_occurrence_id = :routeOccurrenceId
      """,
      nativeQuery = true)
  Optional<Instant> findScheduledDepartureForOccurrence(
      @Param("routeOccurrenceId") long routeOccurrenceId);

  @Query(
      value =
          """
      SELECT b.passenger_app_user_id
      FROM booking.booking b
      JOIN trip.trip t ON t.route_plan_id = b.route_plan_id
        AND (b.route_occurrence_id = t.route_occurrence_id OR t.route_occurrence_id IS NULL)
      WHERE t.trip_id = :tripId AND b.status = 'CONFIRMED'
      """,
      nativeQuery = true)
  List<Long> findConfirmedPassengerAppUserIds(@Param("tripId") long tripId);

  interface DriverTripRow {
    Long getTripId();

    Long getRoutePlanId();

    Long getRouteOccurrenceId();

    String getOriginLabel();

    String getDestinationLabel();

    Instant getDepartureTime();

    String getStatus();

    Long getConfirmedBookings();

    Integer getBookedSeats();

    Instant getStartedAt();

    Instant getCompletedAt();
  }

  default TripStatus findStatusForUpdate(long tripId) {
    return lockAndFindStatus(tripId).orElseThrow();
  }

  /**
   * {@code now} is passed in rather than read here: a trip's started/completed instants are the
   * basis of later timing decisions, and every instant in this slice comes from the injected {@code
   * Clock} so that tests can move time without sleeping and nothing can be influenced off-server.
   */
  default void updateStatus(long tripId, TripStatus status, Instant now) {
    TripEntity trip = findById(tripId).orElseThrow();
    trip.setStatus(status);
    if (status == TripStatus.STARTED) {
      trip.setStartedAt(now);
    }
    if (status == TripStatus.COMPLETED) {
      trip.setCompletedAt(now);
    }
    save(trip);
  }
}
