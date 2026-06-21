package com.routeshare.driver.controller;

import com.routeshare.driver.dto.request.DriverKycUploadRequest;
import com.routeshare.driver.dto.response.DriverVerificationStatusResponse;
import com.routeshare.driver.service.DriverDocumentService;
import com.routeshare.driver.service.DriverVerificationService;
import com.routeshare.routing.service.RouteService;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.dto.UploadUrlResponse;
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
  private final VehicleService vehicleService;
  private final RouteService routeService;
  private final DriverVerificationService verificationService;
  private final DriverDocumentService documentService;

  @GetMapping("/api/v1/driver/verification-status")
  public DriverVerificationStatusResponse verificationStatus() {
    return verificationService.status();
  }

  @PutMapping("/api/v1/driver/kyc/identity")
  public UploadUrlResponse kycIdentity(@Valid @RequestBody DriverKycUploadRequest body) {
    return documentService.createUploadUrl(toUploadRequest("IDENTITY", body));
  }

  @PutMapping("/api/v1/driver/kyc/licence")
  public UploadUrlResponse kycLicence(@Valid @RequestBody DriverKycUploadRequest body) {
    return documentService.createUploadUrl(toUploadRequest("LICENCE", body));
  }

  private UploadUrlRequest toUploadRequest(String documentType, DriverKycUploadRequest body) {
    return new UploadUrlRequest(
        documentType, body.contentType(), body.fileSizeBytes(), body.originalFilename());
  }

  // Driver/vehicle document submit are served by the real DriverDocumentController /
  // VehicleDocumentController (Phase 06.6-B); the workflow_item submit shells were removed here.

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
