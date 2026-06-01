package com.routeshare.booking.repository;

import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.entity.BookingEntity;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
