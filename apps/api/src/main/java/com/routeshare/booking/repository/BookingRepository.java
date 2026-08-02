package com.routeshare.booking.repository;

import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.entity.BookingEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
  @Query(
      value =
          """
      INSERT INTO booking.booking(
        route_plan_id, route_occurrence_id, passenger_app_user_id, seats,
        pickup, dropoff, pickup_route_fraction, dropoff_route_fraction, fare_estimate)
      VALUES (:routePlanId, :routeOccurrenceId, :appUserId, :seats,
        ST_SetSRID(ST_MakePoint(:pickupLng, :pickupLat), 4326),
        ST_SetSRID(ST_MakePoint(:dropLng, :dropLat), 4326),
        :pickupRouteFraction, :dropoffRouteFraction, :fareEstimate)
      RETURNING booking_id
      """,
      nativeQuery = true)
  long insertReturningId(
      @Param("routePlanId") long routePlanId,
      @Param("routeOccurrenceId") long routeOccurrenceId,
      @Param("appUserId") long appUserId,
      @Param("seats") int seats,
      @Param("pickupLng") Double pickupLng,
      @Param("pickupLat") Double pickupLat,
      @Param("dropLng") Double dropLng,
      @Param("dropLat") Double dropLat,
      @Param("pickupRouteFraction") BigDecimal pickupRouteFraction,
      @Param("dropoffRouteFraction") BigDecimal dropoffRouteFraction,
      @Param("fareEstimate") BigDecimal fareEstimate);

  Optional<BigDecimal> findFareEstimateByIdAndPassengerAppUserId(
      long bookingId, long passengerAppUserId);

  @Query(
      value = "SELECT passenger_app_user_id FROM booking.booking WHERE booking_id = :bookingId",
      nativeQuery = true)
  Optional<Long> findPassengerAppUserId(@Param("bookingId") long bookingId);

  /** The occurrence a booking sits on — the key the trip behind it is materialised against. */
  @Query("select b.routeOccurrenceId from BookingEntity b where b.id = :bookingId")
  Optional<Long> findRouteOccurrenceId(@Param("bookingId") long bookingId);

  @Query(
      value =
          """
      SELECT t.trip_id
      FROM booking.booking b
      JOIN trip.trip t ON t.route_occurrence_id = b.route_occurrence_id
      WHERE b.booking_id = :bookingId
      """,
      nativeQuery = true)
  Optional<Long> findTripId(@Param("bookingId") long bookingId);

  /**
   * Whether the car is already moving. This is what separates a free cancel from a priced one, so
   * it is read from the trip's own status rather than inferred from a timestamp on the booking.
   */
  @Query(
      value =
          """
      SELECT EXISTS(
        SELECT 1
        FROM booking.booking b
        JOIN trip.trip t ON t.route_occurrence_id = b.route_occurrence_id
        WHERE b.booking_id = :bookingId
          AND t.status IN ('STARTED', 'ARRIVED_PICKUP', 'PASSENGER_ONBOARD'))
      """,
      nativeQuery = true)
  boolean isTripStartedForBooking(@Param("bookingId") long bookingId);

  /** What this checkout carried over from an earlier trip, kept on the booking for the receipt. */
  @Modifying
  @Query(
      value =
          "UPDATE booking.booking SET applied_dues_amount = :amount WHERE booking_id = :bookingId",
      nativeQuery = true)
  int recordAppliedDues(@Param("bookingId") long bookingId, @Param("amount") BigDecimal amount);

  @Query(
      value =
          """
      SELECT b.fare_estimate
      FROM booking.booking b
      JOIN routing.route_plan r ON r.route_plan_id = b.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      WHERE b.booking_id = :bookingId
        AND d.app_user_id = :driverAppUserId
      """,
      nativeQuery = true)
  Optional<BigDecimal> findFareEstimateForDriverBooking(
      @Param("bookingId") long bookingId, @Param("driverAppUserId") long driverAppUserId);

  @Query(
      value =
          """
      SELECT d.app_user_id
      FROM booking.booking b
      JOIN routing.route_plan r ON r.route_plan_id = b.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      WHERE b.booking_id = :bookingId
        AND b.passenger_app_user_id = :passengerAppUserId
      """,
      nativeQuery = true)
  Optional<Long> findDriverAppUserIdForPassengerBooking(
      @Param("bookingId") long bookingId, @Param("passengerAppUserId") long passengerAppUserId);

  @Query(
      value =
          """
      SELECT status
      FROM booking.booking
      WHERE booking_id = :bookingId
        AND passenger_app_user_id = :passengerAppUserId
      FOR UPDATE
      """,
      nativeQuery = true)
  Optional<String> findStatusForUpdateByIdAndPassengerAppUserId(
      @Param("bookingId") long bookingId, @Param("passengerAppUserId") long passengerAppUserId);

  @Query(
      value =
          """
      SELECT b.status
      FROM booking.booking b
      JOIN routing.route_plan r ON r.route_plan_id = b.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      WHERE b.booking_id = :bookingId
        AND d.app_user_id = :driverAppUserId
      FOR UPDATE OF b
      """,
      nativeQuery = true)
  Optional<String> findStatusForUpdateByIdAndDriverAppUserId(
      @Param("bookingId") long bookingId, @Param("driverAppUserId") long driverAppUserId);

  @Modifying
  @Query(
      value =
          """
      UPDATE booking.booking
      SET status = :status
      WHERE booking_id = :bookingId
      """,
      nativeQuery = true)
  int updateStatus(@Param("bookingId") long bookingId, @Param("status") String status);

  /**
   * Projects the passenger's exit point onto the driver route to support early drop-off
   * finalization.
   */
  @Query(
      value =
          """
      SELECT ST_LineLocatePoint(rp.route_line, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))
               AS "exitFraction",
             rp.route_length_m AS "routeLengthM",
             COALESCE(b.pickup_route_fraction, 0) AS "pickupFraction",
             COALESCE(b.dropoff_route_fraction, 1) AS "dropoffFraction",
             b.seats AS "seats",
             b.status AS "status",
             b.fare_estimate AS "fareEstimate"
      FROM booking.booking b
      JOIN routing.route_plan rp ON rp.route_plan_id = b.route_plan_id
      WHERE b.booking_id = :bookingId AND b.passenger_app_user_id = :passengerAppUserId
      """,
      nativeQuery = true)
  Optional<EarlyDropOffContext> findEarlyDropOffContext(
      @Param("bookingId") long bookingId,
      @Param("passengerAppUserId") long passengerAppUserId,
      @Param("lat") double lat,
      @Param("lng") double lng);

  @Modifying
  @Query(
      value =
          "UPDATE booking.booking SET dropoff_route_fraction = :fraction WHERE booking_id = :bookingId",
      nativeQuery = true)
  int updateDropoffFraction(
      @Param("bookingId") long bookingId, @Param("fraction") java.math.BigDecimal fraction);

  interface EarlyDropOffContext {
    Double getExitFraction();

    java.math.BigDecimal getRouteLengthM();

    java.math.BigDecimal getPickupFraction();

    java.math.BigDecimal getDropoffFraction();

    Integer getSeats();

    String getStatus();

    /** The fare she agreed to, which is what stands when the adjusted-drop allowance is spent. */
    java.math.BigDecimal getFareEstimate();
  }

  @Query(
      value =
          PASSENGER_SELECT
              + """
      WHERE b.passenger_app_user_id = :passengerAppUserId
      ORDER BY b.created_at DESC
      """,
      nativeQuery = true)
  List<PassengerBookingRow> findPassengerBookings(
      @Param("passengerAppUserId") long passengerAppUserId);

  @Query(
      value =
          PASSENGER_SELECT
              + """
      WHERE b.passenger_app_user_id = :passengerAppUserId
        AND b.booking_id = :bookingId
      """,
      nativeQuery = true)
  Optional<PassengerBookingRow> findPassengerBookingDetail(
      @Param("passengerAppUserId") long passengerAppUserId, @Param("bookingId") long bookingId);

  @Query(
      value =
          PASSENGER_SELECT
              + """
      WHERE b.passenger_app_user_id = :passengerAppUserId
        AND b.status IN ('REQUESTED','CONFIRMED')
        AND COALESCE(t.status::text, 'SCHEDULED') NOT IN ('COMPLETED','CANCELLED')
      ORDER BY ro.scheduled_departure_at ASC, b.created_at DESC
      LIMIT 1
      """,
      nativeQuery = true)
  Optional<PassengerBookingRow> findCurrentPassengerTrip(
      @Param("passengerAppUserId") long passengerAppUserId);

  @Query(
      value =
          PASSENGER_SELECT
              + """
      WHERE b.passenger_app_user_id = :passengerAppUserId
        AND (b.status IN ('CANCELLED','REJECTED','COMPLETED')
             OR COALESCE(t.status::text, 'SCHEDULED') IN ('COMPLETED','CANCELLED'))
      ORDER BY b.created_at DESC
      """,
      nativeQuery = true)
  List<PassengerBookingRow> findPassengerTripHistory(
      @Param("passengerAppUserId") long passengerAppUserId);

  /**
   * The bookings one trip start must charge. CONFIRMED only: a request the driver has not accepted
   * has no agreed fare to take.
   */
  @Query(
      value =
          """
      SELECT b.booking_id AS "bookingId", b.fare_estimate AS "fareEstimate"
      FROM booking.booking b
      JOIN trip.trip t ON t.route_plan_id = b.route_plan_id
        AND (t.route_occurrence_id = b.route_occurrence_id OR t.route_occurrence_id IS NULL)
      WHERE t.trip_id = :tripId
        AND b.status = 'CONFIRMED'
      ORDER BY b.booking_id ASC
      """,
      nativeQuery = true)
  List<BookingToChargeRow> findConfirmedBookingsForTrip(@Param("tripId") long tripId);

  interface BookingToChargeRow {
    long getBookingId();

    java.math.BigDecimal getFareEstimate();
  }

  @Transactional
  @Modifying
  @Query(
      value =
          """
      UPDATE booking.booking
      SET payment_method = COALESCE(:paymentMethod, payment_method),
          payment_status = :paymentStatus,
          captured_at = COALESCE(:capturedAt, captured_at)
      WHERE booking_id = :bookingId
      """,
      nativeQuery = true)
  void updatePaymentState(
      @Param("bookingId") long bookingId,
      @Param("paymentMethod") String paymentMethod,
      @Param("paymentStatus") String paymentStatus,
      @Param("capturedAt") java.time.Instant capturedAt);

  @Query(
      value =
          """
      SELECT
        b.booking_id AS "bookingId",
        b.route_plan_id AS "routePlanId",
        b.route_occurrence_id AS "routeOccurrenceId",
        t.trip_id AS "tripId",
        b.passenger_app_user_id AS "passengerAppUserId",
        COALESCE(pp.full_name, au.display_name, au.email, au.phone, 'Passenger') AS "passengerName",
        b.seats AS "seats",
        b.status AS "status",
        b.fare_estimate AS "fareEstimate",
        ST_Y(b.pickup) AS "pickupLatitude",
        ST_X(b.pickup) AS "pickupLongitude",
        ST_Y(b.dropoff) AS "dropoffLatitude",
        ST_X(b.dropoff) AS "dropoffLongitude",
        b.created_at AS "createdAt"
      FROM booking.booking b
      JOIN routing.route_plan r ON r.route_plan_id = b.route_plan_id
      JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      LEFT JOIN trip.trip t ON t.route_plan_id = b.route_plan_id
        AND (t.route_occurrence_id = b.route_occurrence_id OR t.route_occurrence_id IS NULL)
      LEFT JOIN passenger.passenger_profile pp ON pp.app_user_id = b.passenger_app_user_id
      LEFT JOIN identity.app_user au ON au.app_user_id = b.passenger_app_user_id
      WHERE d.app_user_id = :driverAppUserId
        AND (:tripId IS NULL OR t.trip_id = :tripId)
        AND b.status IN ('REQUESTED','CONFIRMED')
      ORDER BY b.created_at ASC
      """,
      nativeQuery = true)
  List<DriverBookingRequestRow> findDriverBookingRequests(
      @Param("driverAppUserId") long driverAppUserId, @Param("tripId") Long tripId);

  default long create(
      long appUserId, BookingRequest request, long routePlanId, BigDecimal fareEstimate) {
    return insertReturningId(
        routePlanId,
        request.routeOccurrenceId(),
        appUserId,
        request.seats(),
        request.pickupLng(),
        request.pickupLat(),
        request.dropLng(),
        request.dropLat(),
        BigDecimal.valueOf(request.pickupRouteFraction()),
        BigDecimal.valueOf(request.dropoffRouteFraction()),
        fareEstimate);
  }

  default Optional<BigDecimal> findFareEstimateByBookingIdAndPassengerAppUserId(
      long bookingId, long appUserId) {
    return findFareEstimateByIdAndPassengerAppUserId(bookingId, appUserId);
  }

  String PASSENGER_SELECT =
      """
      SELECT
        b.booking_id AS "bookingId",
        b.route_plan_id AS "routePlanId",
        b.route_occurrence_id AS "routeOccurrenceId",
        t.trip_id AS "tripId",
        r.origin_label AS "originLabel",
        r.destination_label AS "destinationLabel",
        ro.scheduled_departure_at AS "departureTime",
        b.seats AS "seats",
        b.status AS "bookingStatus",
        t.status AS "tripStatus",
        pts.status AS "passengerTripStatus",
        b.fare_estimate AS "fareEstimate",
        pi.status AS "paymentStatus",
        b.payment_method AS "paymentMethod",
        pi.authorized_at AS "authorizedAt",
        pi.captured_at AS "capturedAt",
        pi.amount AS "paymentAmount",
        pm.last4 AS "cardLast4",
        ST_Y(b.pickup) AS "pickupLatitude",
        ST_X(b.pickup) AS "pickupLongitude",
        ST_Y(b.dropoff) AS "dropoffLatitude",
        ST_X(b.dropoff) AS "dropoffLongitude",
        b.pickup_route_fraction AS "pickupRouteFraction",
        b.dropoff_route_fraction AS "dropoffRouteFraction",
        b.created_at AS "createdAt"
      FROM booking.booking b
      JOIN routing.route_plan r ON r.route_plan_id = b.route_plan_id
      LEFT JOIN routing.route_occurrence ro ON ro.route_occurrence_id = b.route_occurrence_id
      LEFT JOIN trip.trip t ON t.route_plan_id = b.route_plan_id
        AND (t.route_occurrence_id = b.route_occurrence_id OR t.route_occurrence_id IS NULL)
      LEFT JOIN trip.passenger_trip_state pts ON pts.booking_id = b.booking_id
        AND pts.trip_id = t.trip_id
      LEFT JOIN payment.payment_intent pi ON pi.booking_id = b.booking_id
      LEFT JOIN payment.payment_method pm ON pm.payment_method_id = pi.payment_method_id
      """;

  interface PassengerBookingRow {
    Long getBookingId();

    Long getRoutePlanId();

    Long getRouteOccurrenceId();

    Long getTripId();

    String getOriginLabel();

    String getDestinationLabel();

    Instant getDepartureTime();

    Integer getSeats();

    String getBookingStatus();

    String getTripStatus();

    String getPassengerTripStatus();

    BigDecimal getFareEstimate();

    String getPaymentStatus();

    String getPaymentMethod();

    Instant getAuthorizedAt();

    Instant getCapturedAt();

    BigDecimal getPaymentAmount();

    String getCardLast4();

    Double getPickupLatitude();

    Double getPickupLongitude();

    Double getDropoffLatitude();

    Double getDropoffLongitude();

    BigDecimal getPickupRouteFraction();

    BigDecimal getDropoffRouteFraction();

    Instant getCreatedAt();
  }

  interface DriverBookingRequestRow {
    Long getBookingId();

    Long getRoutePlanId();

    Long getRouteOccurrenceId();

    Long getTripId();

    Long getPassengerAppUserId();

    String getPassengerName();

    Integer getSeats();

    String getStatus();

    BigDecimal getFareEstimate();

    Double getPickupLatitude();

    Double getPickupLongitude();

    Double getDropoffLatitude();

    Double getDropoffLongitude();

    Instant getCreatedAt();
  }
}
