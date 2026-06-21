package com.routeshare.booking.repository;

import com.routeshare.booking.entity.TripShareEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripShareRepository extends JpaRepository<TripShareEntity, Long> {

  Optional<TripShareEntity> findByToken(String token);

  @Modifying
  @Query(
      value =
          """
      UPDATE booking.trip_share
      SET revoked = TRUE
      WHERE token = :token AND passenger_app_user_id = :passengerAppUserId
      """,
      nativeQuery = true)
  int revoke(@Param("token") String token, @Param("passengerAppUserId") long passengerAppUserId);

  /** Public, non-sensitive live trip status for a valid (unrevoked, unexpired) share token. */
  @Query(
      value =
          """
      SELECT
        b.booking_id AS "bookingId",
        r.origin_label AS "originLabel",
        r.destination_label AS "destinationLabel",
        ro.scheduled_departure_at AS "departureTime",
        b.status AS "bookingStatus",
        t.status AS "tripStatus",
        pts.status AS "passengerTripStatus",
        COALESCE(d.display_name, dau.display_name, 'Driver') AS "driverName",
        v.registration_number AS "vehiclePlate",
        s.expires_at AS "expiresAt"
      FROM booking.trip_share s
      JOIN booking.booking b ON b.booking_id = s.booking_id
      JOIN routing.route_plan r ON r.route_plan_id = b.route_plan_id
      LEFT JOIN routing.route_occurrence ro ON ro.route_occurrence_id = b.route_occurrence_id
      LEFT JOIN trip.trip t ON t.route_plan_id = b.route_plan_id
        AND (t.route_occurrence_id = b.route_occurrence_id OR t.route_occurrence_id IS NULL)
      LEFT JOIN trip.passenger_trip_state pts ON pts.booking_id = b.booking_id
        AND pts.trip_id = t.trip_id
      LEFT JOIN driver.driver_profile d ON d.driver_profile_id = r.driver_profile_id
      LEFT JOIN identity.app_user dau ON dau.app_user_id = d.app_user_id
      LEFT JOIN vehicle.vehicle v ON v.vehicle_id = r.vehicle_id
      WHERE s.token = :token
        AND s.revoked = FALSE
        AND s.expires_at > :now
      """,
      nativeQuery = true)
  Optional<PublicTripStatusRow> findPublicStatusByToken(
      @Param("token") String token, @Param("now") Instant now);

  interface PublicTripStatusRow {
    Long getBookingId();

    String getOriginLabel();

    String getDestinationLabel();

    Instant getDepartureTime();

    String getBookingStatus();

    String getTripStatus();

    String getPassengerTripStatus();

    String getDriverName();

    String getVehiclePlate();

    Instant getExpiresAt();
  }
}
