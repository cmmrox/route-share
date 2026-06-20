package com.routeshare.admin.service.impl;

import com.routeshare.admin.dto.AdminBookingResponse;
import com.routeshare.admin.dto.AdminBookingStatusHistoryResponse;
import com.routeshare.admin.dto.AdminDriverApplicationResponse;
import com.routeshare.admin.dto.AdminLocationSampleResponse;
import com.routeshare.admin.dto.AdminTripResponse;
import com.routeshare.admin.dto.AdminVehicleResponse;
import com.routeshare.admin.dto.ReportExportRequest;
import com.routeshare.admin.dto.ReportExportResponse;
import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.admin.service.AdminOpsService;
import com.routeshare.booking.entity.BookingEntity;
import com.routeshare.booking.entity.BookingStatusHistoryEntity;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.BookingStatusHistoryRepository;
import com.routeshare.driver.entity.DriverProfileEntity;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.location.repository.LocationSampleRepository;
import com.routeshare.trip.domain.TripStatus;
import com.routeshare.trip.entity.TripEntity;
import com.routeshare.trip.repository.TripRepository;
import com.routeshare.vehicle.entity.VehicleEntity;
import com.routeshare.vehicle.repository.VehicleRepository;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOpsServiceImpl implements AdminOpsService {
  private static final int MAX_LIMIT = 200;

  private final TripRepository trips;
  private final BookingRepository bookings;
  private final BookingStatusHistoryRepository bookingHistory;
  private final DriverProfileRepository drivers;
  private final VehicleRepository vehicles;
  private final LocationSampleRepository locationSamples;
  private final AdminAuditService audit;

  private PageRequest page(int limit) {
    return PageRequest.of(0, Math.min(limit <= 0 ? 50 : limit, MAX_LIMIT));
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminTripResponse> listTrips(int limit) {
    return trips.findAll(page(limit)).stream().map(this::toTrip).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public AdminTripResponse getTrip(long tripId) {
    return toTrip(requireTrip(tripId));
  }

  @Override
  @Transactional
  public AdminTripResponse cancelTrip(long tripId, String reason) {
    var trip = requireTrip(tripId);
    if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
      throw new IllegalStateException("Trip is already " + trip.getStatus());
    }
    trip.setStatus(TripStatus.CANCELLED);
    audit.record(
        "TRIP_CANCELLED",
        "TRIP",
        String.valueOf(tripId),
        reason == null ? null : "{\"reason\":\"" + reason + "\"}");
    return toTrip(trip);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminLocationSampleResponse> locationTrail(long tripId) {
    return locationSamples.findTrailByTripId(tripId).stream()
        .map(
            r ->
                new AdminLocationSampleResponse(
                    r.getLatitude() == null ? 0.0 : r.getLatitude(),
                    r.getLongitude() == null ? 0.0 : r.getLongitude(),
                    r.getSpeedMps(),
                    r.getBearingDegrees(),
                    r.getAccuracyMeters(),
                    r.getRecordedAt()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminBookingResponse> listBookings(int limit) {
    return bookings.findAll(page(limit)).stream().map(this::toBooking).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public AdminBookingResponse getBooking(long bookingId) {
    return toBooking(
        bookings
            .findById(bookingId)
            .orElseThrow(() -> new NoSuchElementException("Booking not found")));
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminBookingStatusHistoryResponse> bookingStatusHistory(long bookingId) {
    return bookingHistory.findByBookingIdOrderByIdAsc(bookingId).stream()
        .map(this::toHistory)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminDriverApplicationResponse> listDriverApplications(int limit) {
    return drivers.findAll(page(limit)).stream().map(this::toDriverApp).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public AdminDriverApplicationResponse getDriverApplication(long driverProfileId) {
    return toDriverApp(
        drivers
            .findById(driverProfileId)
            .orElseThrow(() -> new NoSuchElementException("Driver application not found")));
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminVehicleResponse> listVehicles(int limit) {
    return vehicles.findAll(page(limit)).stream().map(this::toVehicle).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public AdminVehicleResponse getVehicle(long vehicleId) {
    return toVehicle(
        vehicles
            .findById(vehicleId)
            .orElseThrow(() -> new NoSuchElementException("Vehicle not found")));
  }

  @Override
  @Transactional
  public ReportExportResponse requestExport(ReportExportRequest req) {
    String jobId = UUID.randomUUID().toString();
    audit.record(
        "REPORT_EXPORT_REQUESTED",
        "REPORT",
        jobId,
        "{\"reportType\":\"" + req.reportType() + "\"}");
    return new ReportExportResponse(jobId, req.reportType(), "QUEUED", Instant.now());
  }

  private TripEntity requireTrip(long tripId) {
    return trips.findById(tripId).orElseThrow(() -> new NoSuchElementException("Trip not found"));
  }

  private AdminTripResponse toTrip(TripEntity e) {
    return new AdminTripResponse(
        e.getId(),
        e.getRoutePlanId(),
        e.getRouteOccurrenceId(),
        e.getStatus() == null ? null : e.getStatus().name(),
        e.getStartedAt(),
        e.getCompletedAt());
  }

  private AdminBookingResponse toBooking(BookingEntity e) {
    return new AdminBookingResponse(
        e.getId(),
        e.getRoutePlanId(),
        e.getRouteOccurrenceId(),
        e.getPassengerAppUserId(),
        e.getSeats(),
        e.getStatus(),
        e.getFareEstimate());
  }

  private AdminBookingStatusHistoryResponse toHistory(BookingStatusHistoryEntity e) {
    return new AdminBookingStatusHistoryResponse(
        e.getId(),
        e.getFromStatus(),
        e.getToStatus(),
        e.getChangedByAppUserId(),
        e.getReason(),
        e.getChangedAt());
  }

  private AdminDriverApplicationResponse toDriverApp(DriverProfileEntity e) {
    return new AdminDriverApplicationResponse(
        e.getId(), e.getAppUserId(), e.getDisplayName(), e.getVerificationStatus());
  }

  private AdminVehicleResponse toVehicle(VehicleEntity e) {
    return new AdminVehicleResponse(
        e.getId(),
        e.getDriverProfileId(),
        e.getMake(),
        e.getModel(),
        e.getManufactureYear(),
        e.getColor(),
        e.getRegistrationNumber(),
        e.getSeatCount(),
        e.getStatus());
  }
}
