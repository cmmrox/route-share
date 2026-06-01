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

  @GetMapping("/api/v1/admin/dashboard")
  public Map<String, Object> dashboard() {
    return service.dashboard();
  }

  @GetMapping("/api/v1/admin/audit/actions")
  public List<Map<String, Object>> auditActions() {
    return service.auditActions();
  }

  @GetMapping("/api/v1/admin/users")
  public List<Map<String, Object>> users() {
    return service.all("ADMIN_USER_ACTION");
  }

  @GetMapping("/api/v1/admin/users/{appUserId}")
  public Map<String, Object> user(@PathVariable long appUserId) {
    return Map.of("appUserId", appUserId, "status", "ACTIVE");
  }

  @PostMapping("/api/v1/admin/users/{appUserId}/suspend")
  public Map<String, Object> suspendUser(
      @PathVariable long appUserId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "ADMIN_USER_ACTION",
        "ADMIN",
        "APP_USER",
        String.valueOf(appUserId),
        withStatus(body, "SUSPENDED"));
  }

  @PostMapping("/api/v1/admin/users/{appUserId}/activate")
  public Map<String, Object> activateUser(
      @PathVariable long appUserId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "ADMIN_USER_ACTION",
        "ADMIN",
        "APP_USER",
        String.valueOf(appUserId),
        withStatus(body, "ACTIVE"));
  }

  @PutMapping("/api/v1/admin/users/{appUserId}/roles")
  public Map<String, Object> updateRoles(
      @PathVariable long appUserId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "ADMIN_ROLE_UPDATE", "ADMIN", "APP_USER", String.valueOf(appUserId), body);
  }

  @GetMapping("/api/v1/admin/users/{appUserId}/status-history")
  public List<Map<String, Object>> userStatusHistory(@PathVariable long appUserId) {
    return service.all("ADMIN_USER_ACTION");
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

  @PostMapping("/api/v1/admin/driver-documents/{documentId}/review")
  public Map<String, Object> reviewDriverDocument(
      @PathVariable long documentId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "DRIVER_DOCUMENT_REVIEW", "ADMIN", "DRIVER_DOCUMENT", String.valueOf(documentId), body);
  }

  @PostMapping("/api/v1/admin/vehicle-documents/{documentId}/review")
  public Map<String, Object> reviewVehicleDocument(
      @PathVariable long documentId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "VEHICLE_DOCUMENT_REVIEW", "ADMIN", "VEHICLE_DOCUMENT", String.valueOf(documentId), body);
  }

  @GetMapping("/api/v1/admin/vehicles")
  public List<Map<String, Object>> vehicles() {
    return service.all("VEHICLE_DOCUMENT_SUBMISSION");
  }

  @GetMapping("/api/v1/admin/vehicles/{vehicleId}")
  public Map<String, Object> vehicle(@PathVariable long vehicleId) {
    return Map.of("vehicleId", vehicleId, "status", "REVIEWABLE");
  }

  @PostMapping("/api/v1/admin/documents/{documentId}/download-url")
  public Map<String, Object> documentDownloadUrl(@PathVariable long documentId) {
    return Map.of(
        "documentId",
        documentId,
        "downloadUrl",
        "https://routeshare.local/documents/" + documentId,
        "expiresInSeconds",
        300);
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

  @GetMapping("/api/v1/admin/commission-rules")
  public List<Map<String, Object>> commissionRules() {
    return service.all("COMMISSION_RULE");
  }

  @PostMapping("/api/v1/admin/commission-rules")
  public Map<String, Object> createCommissionRule(
      @RequestBody(required = false) Map<String, Object> body) {
    return service.create("COMMISSION_RULE", "ADMIN", "FINANCE", null, body);
  }

  @PutMapping("/api/v1/admin/commission-rules/{ruleId}")
  public Map<String, Object> updateCommissionRule(
      @PathVariable long ruleId, @RequestBody(required = false) Map<String, Object> body) {
    return service.update(ruleId, body);
  }

  @GetMapping("/api/v1/admin/fare-policies")
  public List<Map<String, Object>> farePolicies() {
    return service.all("FARE_POLICY");
  }

  @PostMapping("/api/v1/admin/fare-policies")
  public Map<String, Object> createFarePolicy(
      @RequestBody(required = false) Map<String, Object> body) {
    return service.create("FARE_POLICY", "ADMIN", "FINANCE", null, body);
  }

  @PutMapping("/api/v1/admin/fare-policies/{policyId}")
  public Map<String, Object> updateFarePolicy(
      @PathVariable long policyId, @RequestBody(required = false) Map<String, Object> body) {
    return service.update(policyId, body);
  }

  @GetMapping("/api/v1/admin/settlements/driver-balances")
  public List<Map<String, Object>> driverBalances() {
    return service.all("SETTLEMENT_BALANCE");
  }

  @GetMapping("/api/v1/admin/settlements/payout-batches")
  public List<Map<String, Object>> payoutBatches() {
    return service.all("PAYOUT_BATCH");
  }

  @PostMapping("/api/v1/admin/settlements/payout-batches")
  public Map<String, Object> createPayoutBatch(
      @RequestBody(required = false) Map<String, Object> body) {
    return service.create("PAYOUT_BATCH", "ADMIN", "FINANCE", null, body);
  }

  @PostMapping("/api/v1/admin/settlements/payout-batches/{batchId}/mark-paid")
  public Map<String, Object> markPayoutPaid(
      @PathVariable long batchId, @RequestBody(required = false) Map<String, Object> body) {
    return service.update(batchId, withStatus(body, "PAID"));
  }

  @PostMapping("/api/v1/admin/finance/adjustments")
  public Map<String, Object> financeAdjustment(
      @RequestBody(required = false) Map<String, Object> body) {
    return service.create("FINANCE_ADJUSTMENT", "ADMIN", "FINANCE", null, body);
  }

  @GetMapping("/api/v1/admin/support/tickets")
  public List<Map<String, Object>> supportTickets() {
    return service.all("SUPPORT_TICKET");
  }

  @GetMapping("/api/v1/admin/support/tickets/{ticketId}")
  public Map<String, Object> supportTicket(@PathVariable long ticketId) {
    return service.get(ticketId);
  }

  @PutMapping("/api/v1/admin/support/tickets/{ticketId}")
  public Map<String, Object> updateSupportTicket(
      @PathVariable long ticketId, @RequestBody(required = false) Map<String, Object> body) {
    return service.update(ticketId, body);
  }

  @PostMapping("/api/v1/admin/support/tickets/{ticketId}/messages")
  public Map<String, Object> supportTicketMessage(
      @PathVariable long ticketId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "SUPPORT_MESSAGE", "ADMIN", "SUPPORT_TICKET", String.valueOf(ticketId), body);
  }

  @GetMapping("/api/v1/admin/safety/sos-events")
  public List<Map<String, Object>> sosEvents() {
    return service.all("SOS_EVENT");
  }

  @GetMapping("/api/v1/admin/safety/sos-events/{sosEventId}")
  public Map<String, Object> sosEvent(@PathVariable long sosEventId) {
    return service.get(sosEventId);
  }

  @PostMapping("/api/v1/admin/safety/sos-events/{sosEventId}/resolve")
  public Map<String, Object> resolveSos(
      @PathVariable long sosEventId, @RequestBody(required = false) Map<String, Object> body) {
    return service.update(sosEventId, withStatus(body, "RESOLVED"));
  }

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
