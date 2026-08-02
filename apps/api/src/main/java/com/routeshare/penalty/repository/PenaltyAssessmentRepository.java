package com.routeshare.penalty.repository;

import com.routeshare.penalty.entity.PenaltyAssessmentEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PenaltyAssessmentRepository extends JpaRepository<PenaltyAssessmentEntity, Long> {

  Optional<PenaltyAssessmentEntity> findByKindAndBookingId(String kind, Long bookingId);

  Optional<PenaltyAssessmentEntity> findByKindAndTripIdAndBookingIdIsNull(String kind, Long tripId);

  List<PenaltyAssessmentEntity> findByPayerAppUserIdOrderByAssessedAtDesc(long payerAppUserId);

  /** Admin listing, filtered on either or both of kind and status. */
  @Query(
      """
      SELECT p FROM PenaltyAssessmentEntity p
       WHERE (:kind IS NULL OR p.kind = :kind)
         AND (:status IS NULL OR p.status = :status)
       ORDER BY p.assessedAt DESC
      """)
  List<PenaltyAssessmentEntity> search(@Param("kind") String kind, @Param("status") String status);

  // ── subjects ─────────────────────────────────────────────────────────────────────────────────

  @Query(
      value =
          "SELECT b.passenger_app_user_id FROM booking.booking b WHERE b.booking_id = :bookingId",
      nativeQuery = true)
  Optional<Long> findPassengerAppUserId(@Param("bookingId") long bookingId);

  @Query(
      value =
          """
          SELECT d.app_user_id
            FROM booking.booking b
            JOIN routing.route_plan rp ON rp.route_plan_id = b.route_plan_id
            JOIN driver.driver_profile d ON d.driver_profile_id = rp.driver_profile_id
           WHERE b.booking_id = :bookingId
          """,
      nativeQuery = true)
  Optional<Long> findDriverAppUserIdForBooking(@Param("bookingId") long bookingId);

  @Query(
      value =
          """
          SELECT d.app_user_id
            FROM trip.trip t
            JOIN routing.route_plan rp ON rp.route_plan_id = t.route_plan_id
            JOIN driver.driver_profile d ON d.driver_profile_id = rp.driver_profile_id
           WHERE t.trip_id = :tripId
          """,
      nativeQuery = true)
  Optional<Long> findDriverAppUserIdForTrip(@Param("tripId") long tripId);

  /**
   * Everyone a trip-wide driver penalty let down, in booking order.
   *
   * <p>Ordered here rather than in Java because the remainder of an odd victim half goes to the
   * first of them, and "first" must mean the same thing on every run.
   */
  @Query(
      value =
          """
          SELECT b.booking_id AS "bookingId",
                 b.passenger_app_user_id AS "passengerAppUserId",
                 b.fare_estimate AS "fareEstimate"
            FROM booking.booking b
            JOIN trip.trip t ON t.route_occurrence_id = b.route_occurrence_id
           WHERE t.trip_id = :tripId
             AND b.status = 'CONFIRMED'
           ORDER BY b.booking_id ASC
          """,
      nativeQuery = true)
  List<AffectedBookingRow> findConfirmedBookingsForTrip(@Param("tripId") long tripId);

  // ── bases ────────────────────────────────────────────────────────────────────────────────────

  /**
   * What the passenger actually pays, from the stored quote — never recomputed. Rate bands move and
   * discount tiers are tunable, so a penalty priced months later must still use the fare that was
   * charged. Falls back to the booking's own estimate when no quote exists (pre-slice-03 rows).
   */
  @Query(
      value =
          """
          SELECT COALESCE(
                   (SELECT q.passenger_pays FROM pricing.fare_quote q
                     WHERE q.booking_id = b.booking_id
                     ORDER BY q.fare_quote_id DESC LIMIT 1),
                   b.fare_estimate,
                   0)
            FROM booking.booking b
           WHERE b.booking_id = :bookingId
          """,
      nativeQuery = true)
  Optional<BigDecimal> findPassengerFare(@Param("bookingId") long bookingId);

  /**
   * The driver's net for one seat — the base a driver-late fee is a percentage of, not her fare.
   */
  @Query(
      value =
          """
          SELECT COALESCE(
                   (SELECT q.driver_net FROM pricing.fare_quote q
                     WHERE q.booking_id = b.booking_id
                     ORDER BY q.fare_quote_id DESC LIMIT 1),
                   0)
            FROM booking.booking b
           WHERE b.booking_id = :bookingId
          """,
      nativeQuery = true)
  Optional<BigDecimal> findDriverNetForBooking(@Param("bookingId") long bookingId);

  /** What the whole trip was expected to net him — the base for cancelling it inside the window. */
  @Query(
      value =
          """
          SELECT COALESCE(SUM(
                   COALESCE(
                     (SELECT q.driver_net FROM pricing.fare_quote q
                       WHERE q.booking_id = b.booking_id
                       ORDER BY q.fare_quote_id DESC LIMIT 1),
                     0)), 0)
            FROM booking.booking b
            JOIN trip.trip t ON t.route_occurrence_id = b.route_occurrence_id
           WHERE t.trip_id = :tripId
             AND b.status = 'CONFIRMED'
          """,
      nativeQuery = true)
  BigDecimal findExpectedNetForTrip(@Param("tripId") long tripId);

  /**
   * When the trip was due to leave — the reference point for "inside the free window".
   *
   * <p>Prefers the start window, which is the departure the clocks actually ran against, and falls
   * back to the occurrence's schedule for a trip whose window was never opened.
   */
  @Query(
      value =
          """
          SELECT COALESCE(
                   (SELECT w.departs_at FROM trip.trip_start_window w WHERE w.trip_id = t.trip_id),
                   (SELECT ro.scheduled_departure_at FROM routing.route_occurrence ro
                     WHERE ro.route_occurrence_id = t.route_occurrence_id))
            FROM trip.trip t
           WHERE t.trip_id = :tripId
          """,
      nativeQuery = true)
  Optional<Instant> findDepartureForTrip(@Param("tripId") long tripId);

  // ── display ──────────────────────────────────────────────────────────────────────────────────

  /**
   * The trip a penalty came from, for P25's "which trip, when". Reads through either subject, since
   * a trip-wide penalty has no booking of its own.
   */
  @Query(
      value =
          """
          SELECT rp.origin_label AS "originLabel",
                 rp.destination_label AS "destinationLabel",
                 ro.scheduled_departure_at AS "departsAt"
            FROM booking.booking b
            JOIN routing.route_plan rp ON rp.route_plan_id = b.route_plan_id
            LEFT JOIN routing.route_occurrence ro ON ro.route_occurrence_id = b.route_occurrence_id
           WHERE b.booking_id = :bookingId
          """,
      nativeQuery = true)
  Optional<TripLabelRow> findTripLabelsForBooking(@Param("bookingId") long bookingId);

  /**
   * A beneficiary's first name and nothing else. D31 names "Dinuka and Tharindu" to the driver who
   * owes them; a surname, phone or email would turn a fee notice into a disclosure.
   *
   * <p>A phone-OTP account whose profile is not filled in yet carries its <em>phone number</em> as
   * its display name, so taking the first word of it would print somebody's number on a driver's
   * fee notice. Anything that is not plainly a name falls back to the generic label — the payload
   * is a courtesy, and a missing courtesy is much cheaper than a disclosed contact detail.
   */
  @Query(
      value =
          """
          SELECT CASE
                   WHEN u.display_name IS NULL THEN 'A rider'
                   WHEN u.display_name ~ '[0-9@]' THEN 'A rider'
                   ELSE split_part(btrim(u.display_name), ' ', 1)
                 END
            FROM identity.app_user u
           WHERE u.app_user_id = :appUserId
          """,
      nativeQuery = true)
  Optional<String> findFirstName(@Param("appUserId") long appUserId);

  interface AffectedBookingRow {
    Long getBookingId();

    Long getPassengerAppUserId();

    BigDecimal getFareEstimate();
  }

  interface TripLabelRow {
    String getOriginLabel();

    String getDestinationLabel();

    Instant getDepartsAt();
  }
}
