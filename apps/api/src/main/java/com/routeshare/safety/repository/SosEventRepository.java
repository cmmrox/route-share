package com.routeshare.safety.repository;

import com.routeshare.safety.entity.SosEventEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SosEventRepository extends JpaRepository<SosEventEntity, Long> {
  List<SosEventEntity> findByAppUserIdOrderByIdDesc(long appUserId);

  List<SosEventEntity> findByStatusOrderByIdDesc(String status, Pageable pageable);

  List<SosEventEntity> findAllByOrderByIdDesc(Pageable pageable);

  Optional<SosEventEntity> findByIdAndAppUserId(long id, long appUserId);

  long countByStatus(String status);

  @Query(
      value =
          """
          SELECT b.passenger_app_user_id AS "passengerAppUserId",
                 d.app_user_id AS "driverAppUserId",
                 v.registration_number AS "vehicleRegistration",
                 rp.destination_label AS "destinationLabel"
          FROM trip.trip t
          JOIN routing.route_plan rp ON rp.route_plan_id = t.route_plan_id
          JOIN driver.driver_profile d ON d.driver_profile_id = rp.driver_profile_id
          JOIN vehicle.vehicle v ON v.vehicle_id = rp.vehicle_id
          LEFT JOIN booking.booking b
            ON b.booking_id = :bookingId AND b.route_occurrence_id = t.route_occurrence_id
          WHERE t.trip_id = :tripId
          """,
      nativeQuery = true)
  Optional<SosContextRow> findContext(
      @Param("tripId") long tripId, @Param("bookingId") Long bookingId);

  @Modifying
  @Query(
      value =
          """
          UPDATE safety.sos_event
          SET vehicle_registration = :vehicleRegistration,
              snapshot_location = CASE
                WHEN :latitude IS NULL OR :longitude IS NULL THEN NULL
                ELSE ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)
              END,
              snapshot_place_label = :placeLabel,
              role = :role,
              destination_label = :destinationLabel,
              contacts_alerted = :contactsAlerted,
              contact_alert_failures = :contactAlertFailures
          WHERE sos_event_id = :sosEventId
          """,
      nativeQuery = true)
  int updateSnapshot(
      @Param("sosEventId") long sosEventId,
      @Param("vehicleRegistration") String vehicleRegistration,
      @Param("latitude") Double latitude,
      @Param("longitude") Double longitude,
      @Param("placeLabel") String placeLabel,
      @Param("role") String role,
      @Param("destinationLabel") String destinationLabel,
      @Param("contactsAlerted") int contactsAlerted,
      @Param("contactAlertFailures") int contactAlertFailures);

  interface SosContextRow {
    Long getPassengerAppUserId();

    Long getDriverAppUserId();

    String getVehicleRegistration();

    String getDestinationLabel();
  }
}
