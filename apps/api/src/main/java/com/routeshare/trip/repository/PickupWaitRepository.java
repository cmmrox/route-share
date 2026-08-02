package com.routeshare.trip.repository;

import com.routeshare.trip.entity.PickupWaitEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PickupWaitRepository extends JpaRepository<PickupWaitEntity, Long> {

  Optional<PickupWaitEntity> findByBookingId(long bookingId);

  /** Same claim-under-lock shape as the start-window sweep, and for the same reason. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT w FROM PickupWaitEntity w
       WHERE w.resolvedAt IS NULL
         AND COALESCE(w.extendedExpiresAt, w.expiresAt) <= :now
       ORDER BY COALESCE(w.extendedExpiresAt, w.expiresAt) ASC
      """)
  List<PickupWaitEntity> claimExpired(@Param("now") Instant now, Pageable page);

  /**
   * Confirmed bookings on this trip that have no wait yet. Once a wait exists the clock is running
   * and further samples must not restart it — an arrival is detected once per passenger.
   */
  @Query(
      value =
          """
          SELECT b.booking_id
            FROM booking.booking b
            JOIN trip.trip t ON t.route_occurrence_id = b.route_occurrence_id
            LEFT JOIN trip.pickup_wait pw ON pw.booking_id = b.booking_id
           WHERE t.trip_id = :tripId
             AND b.status = 'CONFIRMED'
             AND pw.pickup_wait_id IS NULL
          """,
      nativeQuery = true)
  List<Long> findBookingsAwaitingArrival(@Param("tripId") long tripId);

  /**
   * The recent trail for one trip, measured against one booking's pickup point.
   *
   * <p>Distance is computed by PostGIS in metres on the geography type rather than in Java: the
   * pickup point is already stored as geometry, and re-deriving the distance from lat/lng in
   * application code would give two answers to the question a disputed no-show turns on.
   */
  @Query(
      value =
          """
          SELECT ls.location_sample_id AS "sampleId",
                 ls.device_recorded_at AS "recordedAt",
                 ST_Distance(ls.point::geography, b.pickup::geography) AS "distanceMeters"
            FROM location.location_sample ls
            JOIN booking.booking b ON b.booking_id = :bookingId
           WHERE ls.trip_id = :tripId
             AND ls.device_recorded_at >= :since
           ORDER BY ls.device_recorded_at ASC
          """,
      nativeQuery = true)
  List<TrailSampleRow> findTrailAgainstPickup(
      @Param("tripId") long tripId,
      @Param("bookingId") long bookingId,
      @Param("since") Instant since);

  /** The passenger a pickup wait belongs to, for the "your driver is here" notification. */
  @Query(
      value =
          """
          SELECT b.passenger_app_user_id
            FROM booking.booking b
           WHERE b.booking_id = :bookingId
          """,
      nativeQuery = true)
  Optional<Long> findPassengerAppUserId(@Param("bookingId") long bookingId);

  /** The driver a released seat credits, and whose trip the wait sits on. */
  @Query(
      value =
          """
          SELECT dp.app_user_id
            FROM trip.trip t
            JOIN routing.route_plan rp ON rp.route_plan_id = t.route_plan_id
            JOIN driver.driver_profile dp ON dp.driver_profile_id = rp.driver_profile_id
           WHERE t.trip_id = :tripId
          """,
      nativeQuery = true)
  Optional<Long> findDriverAppUserId(@Param("tripId") long tripId);

  /** Ownership: this trip belongs to this driver. */
  @Query(
      value =
          """
          SELECT EXISTS(
            SELECT 1 FROM trip.trip t
              JOIN routing.route_plan rp ON rp.route_plan_id = t.route_plan_id
              JOIN driver.driver_profile dp ON dp.driver_profile_id = rp.driver_profile_id
             WHERE t.trip_id = :tripId AND dp.app_user_id = :appUserId)
          """,
      nativeQuery = true)
  boolean isTripOwnedByDriverAppUser(
      @Param("tripId") long tripId, @Param("appUserId") long appUserId);

  /** Ownership: this booking is hers. A pickup window states her fee and her no-show count. */
  @Query(
      value =
          """
          SELECT EXISTS(
            SELECT 1 FROM booking.booking b
             WHERE b.booking_id = :bookingId AND b.passenger_app_user_id = :appUserId)
          """,
      nativeQuery = true)
  boolean isBookingOwnedByPassengerAppUser(
      @Param("bookingId") long bookingId, @Param("appUserId") long appUserId);

  /** The wait must belong to the trip it is being acted on through. */
  @Query(
      "select count(w) > 0 from PickupWaitEntity w where w.bookingId = :bookingId and w.tripId = :tripId")
  boolean existsForTripAndBooking(@Param("tripId") long tripId, @Param("bookingId") long bookingId);

  /** The seats a booking holds and the occurrence they came from, for a release. */
  @Query(
      value =
          """
          SELECT b.route_occurrence_id AS "routeOccurrenceId", b.seats AS "seats"
            FROM booking.booking b
           WHERE b.booking_id = :bookingId
          """,
      nativeQuery = true)
  Optional<BookingSeatHoldRow> findBookingSeatHold(@Param("bookingId") long bookingId);

  interface BookingSeatHoldRow {
    Long getRouteOccurrenceId();

    Integer getSeats();
  }

  interface TrailSampleRow {
    Long getSampleId();

    Instant getRecordedAt();

    Double getDistanceMeters();
  }
}
