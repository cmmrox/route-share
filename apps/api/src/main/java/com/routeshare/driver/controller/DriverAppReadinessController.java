package com.routeshare.driver.controller;

import com.routeshare.appreadiness.service.AppReadinessService;
import com.routeshare.routing.service.RouteService;
import com.routeshare.vehicle.dto.request.VehicleRequest;
import com.routeshare.vehicle.dto.response.VehicleResponse;
import com.routeshare.vehicle.service.VehicleService;
import jakarta.validation.Valid;
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

  // Recurring routes are served by the real DriverRecurringRouteController and payout profile by
  // DriverPayoutController (Phase 06.6-F); their workflow_item-backed versions were removed here.

  // Ratings, SOS, support tickets, notifications, preferences, and push registrations are served by
  // the real rating/safety/support/notification modules (Phase 06.6-D/E); their
  // workflow_item-backed
  // versions were removed here.
}
