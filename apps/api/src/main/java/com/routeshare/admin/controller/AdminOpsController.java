package com.routeshare.admin.controller;

import com.routeshare.admin.dto.AdminActionRequest;
import com.routeshare.admin.dto.AdminBookingResponse;
import com.routeshare.admin.dto.AdminBookingStatusHistoryResponse;
import com.routeshare.admin.dto.AdminDriverApplicationResponse;
import com.routeshare.admin.dto.AdminTripResponse;
import com.routeshare.admin.dto.AdminVehicleResponse;
import com.routeshare.admin.dto.ReportExportRequest;
import com.routeshare.admin.dto.ReportExportResponse;
import com.routeshare.admin.service.AdminOpsService;
import com.routeshare.common.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','OPS_ADMIN','VERIFICATION_AGENT','FINANCE_ADMIN')")
public class AdminOpsController {
  private final AdminOpsService service;

  public AdminOpsController(AdminOpsService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/admin/trips")
  ApiResponse<List<AdminTripResponse>> trips(
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ApiResponse.ok(service.listTrips(limit));
  }

  @GetMapping("/api/v1/admin/trips/{tripId}")
  ApiResponse<AdminTripResponse> trip(@PathVariable long tripId) {
    return ApiResponse.ok(service.getTrip(tripId));
  }

  @PostMapping("/api/v1/admin/trips/{tripId}/cancel")
  ApiResponse<AdminTripResponse> cancelTrip(
      @PathVariable long tripId, @RequestBody(required = false) AdminActionRequest req) {
    return ApiResponse.ok(service.cancelTrip(tripId, req == null ? null : req.reason()));
  }

  @GetMapping("/api/v1/admin/bookings")
  ApiResponse<List<AdminBookingResponse>> bookings(
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ApiResponse.ok(service.listBookings(limit));
  }

  @GetMapping("/api/v1/admin/bookings/{bookingId}")
  ApiResponse<AdminBookingResponse> booking(@PathVariable long bookingId) {
    return ApiResponse.ok(service.getBooking(bookingId));
  }

  @GetMapping("/api/v1/admin/bookings/{bookingId}/status-history")
  ApiResponse<List<AdminBookingStatusHistoryResponse>> bookingStatusHistory(
      @PathVariable long bookingId) {
    return ApiResponse.ok(service.bookingStatusHistory(bookingId));
  }

  @GetMapping("/api/v1/admin/driver-applications")
  ApiResponse<List<AdminDriverApplicationResponse>> driverApplications(
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ApiResponse.ok(service.listDriverApplications(limit));
  }

  @GetMapping("/api/v1/admin/driver-applications/{driverId}")
  ApiResponse<AdminDriverApplicationResponse> driverApplication(@PathVariable long driverId) {
    return ApiResponse.ok(service.getDriverApplication(driverId));
  }

  @GetMapping("/api/v1/admin/vehicles")
  ApiResponse<List<AdminVehicleResponse>> vehicles(
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ApiResponse.ok(service.listVehicles(limit));
  }

  @GetMapping("/api/v1/admin/vehicles/{vehicleId}")
  ApiResponse<AdminVehicleResponse> vehicle(@PathVariable long vehicleId) {
    return ApiResponse.ok(service.getVehicle(vehicleId));
  }

  @PostMapping("/api/v1/admin/reports/export")
  ApiResponse<ReportExportResponse> exportReport(@Valid @RequestBody ReportExportRequest req) {
    return ApiResponse.ok(service.requestExport(req));
  }
}
