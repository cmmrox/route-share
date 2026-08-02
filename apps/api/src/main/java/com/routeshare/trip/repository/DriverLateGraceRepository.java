package com.routeshare.trip.repository;

import com.routeshare.trip.entity.DriverLateGraceEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverLateGraceRepository extends JpaRepository<DriverLateGraceEntity, Long> {

  Optional<DriverLateGraceEntity> findByBookingId(long bookingId);

  /**
   * Graces past their deadline that have not been unlocked yet. Unlocked rows drop out of the
   * partial index rather than being swept forever.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT g FROM DriverLateGraceEntity g
       WHERE g.resolvedAt IS NULL
         AND g.unlockedAt IS NULL
         AND g.graceExpiresAt <= :now
       ORDER BY g.graceExpiresAt ASC
      """)
  List<DriverLateGraceEntity> claimExpired(@Param("now") Instant now, Pageable page);

  /**
   * The promised pickup time, derived from the occurrence's scheduled departure and how far along
   * the driver's road she gets on. Server-side and deterministic: a promised time the client sent
   * would be a deadline the client chose, and this one decides whether her cancel is free.
   */
  @Query(
      value =
          """
          SELECT ro.scheduled_departure_at
                   + make_interval(secs => (rp.route_length_m * b.pickup_route_fraction)
                                           / (:averageSpeedKmh * 1000.0 / 3600.0))
            FROM booking.booking b
            JOIN routing.route_occurrence ro ON ro.route_occurrence_id = b.route_occurrence_id
            JOIN routing.route_plan rp ON rp.route_plan_id = b.route_plan_id
           WHERE b.booking_id = :bookingId
          """,
      nativeQuery = true)
  Optional<Instant> computePromisedPickupAt(
      @Param("bookingId") long bookingId, @Param("averageSpeedKmh") double averageSpeedKmh);

  /** Stamps the derived time onto the booking so every reader sees the same promise. */
  @Modifying
  @Query(
      value =
          """
          UPDATE booking.booking
             SET promised_pickup_at = :promisedPickupAt
           WHERE booking_id = :bookingId
             AND promised_pickup_at IS NULL
          """,
      nativeQuery = true)
  int stampPromisedPickupAt(
      @Param("bookingId") long bookingId, @Param("promisedPickupAt") Instant promisedPickupAt);

  /**
   * Whether the driver has actually reached this passenger. The grace protects her from a driver
   * who has not turned up; a detected arrival means he has, whatever the clock says.
   */
  @Query(
      value =
          """
          SELECT EXISTS(
            SELECT 1 FROM trip.pickup_wait pw WHERE pw.booking_id = :bookingId)
          """,
      nativeQuery = true)
  boolean hasDriverArrived(@Param("bookingId") long bookingId);

  @Query(
      value =
          """
          SELECT b.passenger_app_user_id
            FROM booking.booking b
           WHERE b.booking_id = :bookingId
          """,
      nativeQuery = true)
  Optional<Long> findPassengerAppUserId(@Param("bookingId") long bookingId);

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

  /** Departure and booking status, for the free-cancellation window that predates the grace. */
  @Query(
      value =
          """
          SELECT ro.scheduled_departure_at AS "departsAt",
                 b.status AS "bookingStatus",
                 b.promised_pickup_at AS "promisedPickupAt"
            FROM booking.booking b
            JOIN routing.route_occurrence ro ON ro.route_occurrence_id = b.route_occurrence_id
           WHERE b.booking_id = :bookingId
          """,
      nativeQuery = true)
  Optional<CancellationContextRow> findCancellationContext(@Param("bookingId") long bookingId);

  interface CancellationContextRow {
    Instant getDepartsAt();

    String getBookingStatus();

    Instant getPromisedPickupAt();
  }
}
