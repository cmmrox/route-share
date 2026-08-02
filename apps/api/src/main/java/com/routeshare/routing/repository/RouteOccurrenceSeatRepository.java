package com.routeshare.routing.repository;

import com.routeshare.routing.entity.RouteOccurrenceSeatEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteOccurrenceSeatRepository
    extends JpaRepository<RouteOccurrenceSeatEntity, Long> {

  List<RouteOccurrenceSeatEntity> findByRouteOccurrenceIdOrderBySlotIndexAsc(
      long routeOccurrenceId);

  /**
   * The seat map as P08 needs it: every slot, and whether somebody is holding it right now.
   *
   * <p>Held-ness is read from the live hold rather than from a flag on the seat, so a released
   * booking puts its seat back on the screen without anything having to remember to.
   */
  @Query(
      value =
          """
      SELECT s.route_occurrence_seat_id AS "seatId",
             s.slot_index AS "slotIndex",
             s.label AS "label",
             s.sub_label AS "subLabel",
             EXISTS (SELECT 1 FROM booking.booking_seat bs
                      WHERE bs.route_occurrence_seat_id = s.route_occurrence_seat_id
                        AND bs.released_at IS NULL) AS "taken"
        FROM routing.route_occurrence_seat s
       WHERE s.route_occurrence_id = :routeOccurrenceId
       ORDER BY s.slot_index
      """,
      nativeQuery = true)
  List<SeatMapRow> findSeatMap(@Param("routeOccurrenceId") long routeOccurrenceId);

  /** Free slots, lowest first — what a booking that did not name its seats is given. */
  @Query(
      value =
          """
      SELECT s.route_occurrence_seat_id
        FROM routing.route_occurrence_seat s
       WHERE s.route_occurrence_id = :routeOccurrenceId
         AND NOT EXISTS (SELECT 1 FROM booking.booking_seat bs
                          WHERE bs.route_occurrence_seat_id = s.route_occurrence_seat_id
                            AND bs.released_at IS NULL)
       ORDER BY s.slot_index
       LIMIT :limit
      """,
      nativeQuery = true)
  List<Long> findFreeSeatIds(
      @Param("routeOccurrenceId") long routeOccurrenceId, @Param("limit") int limit);

  /** Confirms the named slots belong to this occurrence before anything tries to hold them. */
  @Query(
      value =
          """
      SELECT count(*) FROM routing.route_occurrence_seat s
       WHERE s.route_occurrence_id = :routeOccurrenceId
         AND s.route_occurrence_seat_id IN (:seatIds)
      """,
      nativeQuery = true)
  int countSeatsOnOccurrence(
      @Param("routeOccurrenceId") long routeOccurrenceId,
      @Param("seatIds") Collection<Long> seatIds);

  /**
   * Seats currently held on an occurrence. The freeze rule (D09) and the "is it sold out" question
   * both read this, so neither can disagree with the other.
   */
  @Query(
      value =
          """
      SELECT count(*) FROM booking.booking_seat bs
        JOIN routing.route_occurrence_seat s
          ON s.route_occurrence_seat_id = bs.route_occurrence_seat_id
       WHERE s.route_occurrence_id = :routeOccurrenceId
         AND bs.released_at IS NULL
      """,
      nativeQuery = true)
  int countLiveHolds(@Param("routeOccurrenceId") long routeOccurrenceId);

  /**
   * Holds still live behind a booking that has reached a terminal state.
   *
   * <p>A leaked hold silently removes a seat from a car for ever, and nobody involved notices — the
   * driver sees a full trip and the rider sees no availability. This is the reconciliation read
   * behind that alert.
   */
  @Query(
      value =
          """
      SELECT count(*) FROM booking.booking_seat bs
        JOIN booking.booking b ON b.booking_id = bs.booking_id
       WHERE bs.released_at IS NULL
         AND b.status IN ('CANCELLED', 'REJECTED', 'EXPIRED')
      """,
      nativeQuery = true)
  int countLeakedHolds();

  @Query(
      value = "SELECT count(*) FROM routing.route_occurrence_seat WHERE route_occurrence_id = :id",
      nativeQuery = true)
  int countSlots(@Param("id") long routeOccurrenceId);

  /** The vehicle's passenger capacity behind an occurrence, for generating its slots. */
  @Query(
      value =
          """
      SELECT p.available_seats
        FROM routing.route_occurrence o
        JOIN routing.route_plan p ON p.route_plan_id = o.route_plan_id
       WHERE o.route_occurrence_id = :routeOccurrenceId
      """,
      nativeQuery = true)
  Optional<Integer> findCapacity(@Param("routeOccurrenceId") long routeOccurrenceId);

  interface SeatMapRow {
    Long getSeatId();

    Integer getSlotIndex();

    String getLabel();

    String getSubLabel();

    Boolean getTaken();
  }

  /** Occurrence, status and departure — the three facts the freeze and cancellation rules read. */
  @Query(
      value =
          """
      SELECT o.route_occurrence_id AS "occurrenceId",
             o.status AS "status",
             o.scheduled_departure_at AS "departsAt",
             o.approval_mode AS "approvalMode",
             p.driver_profile_id AS "driverProfileId",
             d.app_user_id AS "driverAppUserId"
        FROM routing.route_occurrence o
        JOIN routing.route_plan p ON p.route_plan_id = o.route_plan_id
        JOIN driver.driver_profile d ON d.driver_profile_id = p.driver_profile_id
       WHERE o.route_occurrence_id = :routeOccurrenceId
      """,
      nativeQuery = true)
  Optional<OccurrenceContextRow> findOccurrenceContext(
      @Param("routeOccurrenceId") long routeOccurrenceId);

  interface OccurrenceContextRow {
    Long getOccurrenceId();

    String getStatus();

    Instant getDepartsAt();

    String getApprovalMode();

    Long getDriverProfileId();

    Long getDriverAppUserId();
  }
}
