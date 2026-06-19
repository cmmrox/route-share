package com.routeshare.admin.controller;

import com.routeshare.appreadiness.service.AppReadinessService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize(
    "hasAnyRole('ADMIN','SUPER_ADMIN','OPS_ADMIN','SUPPORT_AGENT','FINANCE_ADMIN','VERIFICATION_AGENT')")
public class AdminAppReadinessController {
  private final AppReadinessService service;

  // Dashboard + reports/summary are served by the real AdminDashboardController (Phase 06.6-G3).

  // Audit log, user list/detail/suspend/activate/status-history are served by the real
  // AdminAuditController and AdminUserController (Phase 06.6-G). Role updates remain here pending
  // Keycloak admin-client propagation.

  @PutMapping("/api/v1/admin/users/{appUserId}/roles")
  public Map<String, Object> updateRoles(
      @PathVariable long appUserId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "ADMIN_ROLE_UPDATE", "ADMIN", "APP_USER", String.valueOf(appUserId), body);
  }

  @GetMapping("/api/v1/admin/driver-applications")
  public List<Map<String, Object>> driverApplications() {
    return service.all("DRIVER_KYC_IDENTITY");
  }

  @GetMapping("/api/v1/admin/driver-applications/{driverId}")
  public Map<String, Object> driverApplication(@PathVariable long driverId) {
    return Map.of("driverId", driverId, "status", "PENDING_REVIEW");
  }

  @PostMapping("/api/v1/admin/driver-applications/{driverId}/review")
  public Map<String, Object> reviewDriverApplication(
      @PathVariable long driverId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "DRIVER_APPLICATION_REVIEW", "ADMIN", "DRIVER", String.valueOf(driverId), body);
  }

  // Driver/vehicle/passenger document review + signed download are served by the real
  // AdminDocumentController (Phase 06.6-G3), backed by the document tables + object storage.

  @GetMapping("/api/v1/admin/vehicles")
  public List<Map<String, Object>> vehicles() {
    return service.all("VEHICLE_DOCUMENT_SUBMISSION");
  }

  @GetMapping("/api/v1/admin/vehicles/{vehicleId}")
  public Map<String, Object> vehicle(@PathVariable long vehicleId) {
    return Map.of("vehicleId", vehicleId, "status", "REVIEWABLE");
  }

  @GetMapping("/api/v1/admin/trips")
  public List<Map<String, Object>> trips() {
    return service.all("ADMIN_TRIP_ACTION");
  }

  @GetMapping("/api/v1/admin/trips/{tripId}")
  public Map<String, Object> trip(@PathVariable long tripId) {
    return Map.of("tripId", tripId, "status", "AVAILABLE_BY_TRIP_MODULE");
  }

  @PostMapping("/api/v1/admin/trips/{tripId}/cancel")
  public Map<String, Object> cancelTrip(
      @PathVariable long tripId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "ADMIN_TRIP_ACTION",
        "ADMIN",
        "TRIP",
        String.valueOf(tripId),
        withStatus(body, "CANCELLED"));
  }

  @GetMapping("/api/v1/admin/trips/{tripId}/location-trail")
  public List<Map<String, Object>> locationTrail(@PathVariable long tripId) {
    return service.all("LOCATION_TRAIL_REQUEST");
  }

  @GetMapping("/api/v1/admin/bookings")
  public List<Map<String, Object>> bookings() {
    return service.all("ADMIN_BOOKING_ACTION");
  }

  @GetMapping("/api/v1/admin/bookings/{bookingId}")
  public Map<String, Object> booking(@PathVariable long bookingId) {
    return Map.of("bookingId", bookingId, "status", "AVAILABLE_BY_BOOKING_MODULE");
  }

  @GetMapping("/api/v1/admin/bookings/{bookingId}/status-history")
  public List<Map<String, Object>> bookingStatusHistory(@PathVariable long bookingId) {
    return service.all("ADMIN_BOOKING_ACTION");
  }

  // Commission rules, fare policies, settlements/payout batches, and finance adjustments are served
  // by the real AdminFinanceController (Phase 06.6-G2), backed by the finance schema.

  // Admin support (tickets/messages/status) and safety (SOS list/detail/resolve) are served by the
  // real AdminSupportController and AdminSafetyController (Phase 06.6-G), backed by the support and
  // safety tables.

  @PostMapping("/api/v1/admin/notifications/broadcasts")
  public Map<String, Object> broadcast(@RequestBody(required = false) Map<String, Object> body) {
    return service.create("NOTIFICATION", "ADMIN", "BROADCAST", null, body);
  }

  @GetMapping("/api/v1/admin/reports/summary")
  public Map<String, Object> reportSummary() {
    return service.dashboard();
  }

  @PostMapping("/api/v1/admin/reports/export")
  public Map<String, Object> reportExport(@RequestBody(required = false) Map<String, Object> body) {
    return service.create("REPORT_EXPORT", "ADMIN", "REPORT", null, body);
  }

  private Map<String, Object> withStatus(Map<String, Object> body, String status) {
    var copy = new java.util.LinkedHashMap<String, Object>(body == null ? Map.of() : body);
    copy.put("status", status);
    return copy;
  }
}
