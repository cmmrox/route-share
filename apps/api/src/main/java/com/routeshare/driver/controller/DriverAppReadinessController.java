package com.routeshare.driver.controller;

import com.routeshare.appreadiness.service.AppReadinessService;
import com.routeshare.routing.service.RouteService;
import com.routeshare.vehicle.dto.request.VehicleRequest;
import com.routeshare.vehicle.dto.response.VehicleResponse;
import com.routeshare.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DriverAppReadinessController {
  private final AppReadinessService service;
  private final VehicleService vehicleService;
  private final RouteService routeService;

  @GetMapping("/api/v1/driver/verification-status")
  public Map<String, Object> verificationStatus() {
    return service.verificationStatus();
  }

  @PutMapping("/api/v1/driver/kyc/identity")
  public Map<String, Object> kycIdentity(@RequestBody(required = false) Map<String, Object> body) {
    return service.create("DRIVER_KYC_IDENTITY", "DRIVER", "DRIVER", null, body);
  }

  @PutMapping("/api/v1/driver/kyc/licence")
  public Map<String, Object> kycLicence(@RequestBody(required = false) Map<String, Object> body) {
    return service.create("DRIVER_KYC_LICENCE", "DRIVER", "DRIVER", null, body);
  }

  @PostMapping("/api/v1/driver/documents/{documentId}/submit")
  public Map<String, Object> submitDriverDocument(
      @PathVariable long documentId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "DRIVER_DOCUMENT_SUBMISSION",
        "DRIVER",
        "DRIVER_DOCUMENT",
        String.valueOf(documentId),
        body);
  }

  @GetMapping("/api/v1/driver/vehicles/{vehicleId}")
  public VehicleResponse vehicle(@PathVariable long vehicleId) {
    return vehicleService.getMine(vehicleId);
  }

  @PutMapping("/api/v1/driver/vehicles/{vehicleId}")
  public VehicleResponse updateVehicle(
      @PathVariable long vehicleId, @Valid @RequestBody VehicleRequest req) {
    return vehicleService.updateMine(vehicleId, req);
  }

  @DeleteMapping("/api/v1/driver/vehicles/{vehicleId}")
  public Map<String, Object> deleteVehicle(@PathVariable long vehicleId) {
    vehicleService.deleteMine(vehicleId);
    return Map.of("deleted", true, "vehicleId", vehicleId);
  }

  @PostMapping("/api/v1/driver/vehicles/{vehicleId}/documents/{documentId}/submit")
  public Map<String, Object> submitVehicleDocument(
      @PathVariable long vehicleId,
      @PathVariable long documentId,
      @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "VEHICLE_DOCUMENT_SUBMISSION",
        "DRIVER",
        "VEHICLE_DOCUMENT",
        vehicleId + ":" + documentId,
        body);
  }

  @PostMapping("/api/v1/driver/routes/{routeId}/publish")
  public Map<String, Object> publishRoute(@PathVariable long routeId) {
    return Map.of(
        "routeId", routeId, "status", "PUBLISHED", "route", routeService.getDriverRoute(routeId));
  }

  @PostMapping("/api/v1/driver/recurring-routes")
  public Map<String, Object> createRecurringRoute(
      @RequestBody(required = false) Map<String, Object> body) {
    return service.create("RECURRING_ROUTE", "DRIVER", "DRIVER", null, body);
  }

  @GetMapping("/api/v1/driver/recurring-routes")
  public List<Map<String, Object>> recurringRoutes() {
    return service.mine("RECURRING_ROUTE", "DRIVER");
  }

  @PutMapping("/api/v1/driver/recurring-routes/{routeId}")
  public Map<String, Object> updateRecurringRoute(
      @PathVariable long routeId, @RequestBody(required = false) Map<String, Object> body) {
    return service.update(routeId, body);
  }

  @DeleteMapping("/api/v1/driver/recurring-routes/{routeId}")
  public Map<String, Object> deleteRecurringRoute(@PathVariable long routeId) {
    return service.update(routeId, Map.of("status", "PAUSED"));
  }

  @PostMapping("/api/v1/driver/recurring-routes/{routeId}/generate-occurrences")
  public Map<String, Object> generateRecurringOccurrences(
      @PathVariable long routeId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "RECURRING_ROUTE_GENERATION", "DRIVER", "RECURRING_ROUTE", String.valueOf(routeId), body);
  }

  @GetMapping("/api/v1/driver/payout-profile")
  public Map<String, Object> payoutProfile() {
    return service.payoutProfile();
  }

  @PutMapping("/api/v1/driver/payout-profile")
  public Map<String, Object> updatePayoutProfile(
      @RequestBody(required = false) Map<String, Object> body) {
    return service.payoutProfile(body);
  }

  @GetMapping("/api/v1/driver/ratings")
  public List<Map<String, Object>> ratings() {
    return service.mine("RATING", "DRIVER");
  }

  @PostMapping("/api/v1/driver/sos-events")
  public Map<String, Object> createSos(@RequestBody(required = false) Map<String, Object> body) {
    return service.create("SOS_EVENT", "DRIVER", "DRIVER", null, body);
  }

  @GetMapping("/api/v1/driver/notifications")
  public List<Map<String, Object>> notifications() {
    return service.mine("NOTIFICATION", "DRIVER");
  }

  @PostMapping("/api/v1/driver/notifications/{notificationId}/read")
  public Map<String, Object> readNotification(@PathVariable long notificationId) {
    return service.markRead(notificationId);
  }

  @GetMapping("/api/v1/driver/notification-preferences")
  public Map<String, Object> notificationPreferences() {
    return service.preferences("DRIVER");
  }

  @PutMapping("/api/v1/driver/notification-preferences")
  public Map<String, Object> updateNotificationPreferences(
      @RequestBody(required = false) Map<String, Object> body) {
    return service.savePreferences("DRIVER", body);
  }

  @PostMapping("/api/v1/driver/push-registrations")
  public Map<String, Object> pushRegistration(
      @RequestBody(required = false) Map<String, Object> body) {
    return service.pushRegistration("DRIVER", body);
  }

  @PostMapping("/api/v1/driver/support/tickets")
  public Map<String, Object> createSupportTicket(
      @RequestBody(required = false) Map<String, Object> body) {
    return service.create("SUPPORT_TICKET", "DRIVER", "DRIVER", null, body);
  }

  @GetMapping("/api/v1/driver/support/tickets")
  public List<Map<String, Object>> supportTickets() {
    return service.mine("SUPPORT_TICKET", "DRIVER");
  }

  @GetMapping("/api/v1/driver/support/tickets/{ticketId}")
  public Map<String, Object> supportTicket(@PathVariable long ticketId) {
    return service.get(ticketId);
  }

  @PostMapping("/api/v1/driver/support/tickets/{ticketId}/messages")
  public Map<String, Object> supportTicketMessage(
      @PathVariable long ticketId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "SUPPORT_MESSAGE", "DRIVER", "SUPPORT_TICKET", String.valueOf(ticketId), body);
  }
}
