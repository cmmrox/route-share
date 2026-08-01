package com.routeshare.admin.controller;

import com.routeshare.admin.dto.DriverDeactivationCommand;
import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.driver.domain.DriverVerificationStatus;
import com.routeshare.driver.dto.response.DriverDeactivationResponse;
import com.routeshare.driver.dto.response.DriverProfileResponse;
import com.routeshare.driver.service.DriverDeactivationService;
import com.routeshare.driver.service.DriverService;
import com.routeshare.identity.facade.IdentityFacade;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','VERIFICATION_AGENT','SUPER_ADMIN')")
public class AdminDriverReviewController {
  private final DriverService service;
  private final DriverDeactivationService deactivations;
  private final AdminAuditService audit;
  private final CurrentUserProvider currentUsers;
  private final IdentityFacade identity;

  public AdminDriverReviewController(
      DriverService service,
      DriverDeactivationService deactivations,
      AdminAuditService audit,
      CurrentUserProvider currentUsers,
      IdentityFacade identity) {
    this.service = service;
    this.deactivations = deactivations;
    this.audit = audit;
    this.currentUsers = currentUsers;
    this.identity = identity;
  }

  @PostMapping("/api/v1/admin/drivers/{id}/review")
  ApiResponse<DriverProfileResponse> review(
      @PathVariable long id, @RequestParam DriverVerificationStatus status) {
    return ApiResponse.ok(reviewDriver(id, status));
  }

  /** Legacy admin-web path; consolidated onto the real driver-verification review. */
  @PostMapping("/api/v1/admin/driver-applications/{id}/review")
  ApiResponse<DriverProfileResponse> reviewApplication(
      @PathVariable long id, @RequestParam DriverVerificationStatus status) {
    return ApiResponse.ok(reviewDriver(id, status));
  }

  /**
   * Stops a driver driving without touching their rider account. Slice 05's automatic
   * three-missed-starts trigger calls the same service rather than growing a second path.
   */
  @PostMapping("/api/v1/admin/drivers/{driverProfileId}/deactivate")
  ApiResponse<DriverDeactivationResponse> deactivate(
      @PathVariable long driverProfileId, @Valid @RequestBody DriverDeactivationCommand req) {
    var result =
        deactivations.deactivate(
            driverProfileId, req.reason(), req.caseRef(), currentAdminAppUserId());
    audit.record(
        "DRIVER_DEACTIVATED",
        "driver_profile",
        String.valueOf(driverProfileId),
        "{\"caseRef\":\"" + req.caseRef() + "\",\"reason\":\"" + req.reason() + "\"}");
    return ApiResponse.ok(result);
  }

  @PostMapping("/api/v1/admin/drivers/{driverProfileId}/reinstate")
  ApiResponse<DriverDeactivationResponse> reinstate(
      @PathVariable long driverProfileId, @RequestParam(required = false) String note) {
    var result = deactivations.reinstate(driverProfileId, currentAdminAppUserId(), note);
    audit.record(
        "DRIVER_REINSTATED",
        "driver_profile",
        String.valueOf(driverProfileId),
        "{\"caseRef\":\"" + result.caseRef() + "\"}");
    return ApiResponse.ok(result);
  }

  private long currentAdminAppUserId() {
    return identity.upsertFromToken(currentUsers.requireCurrentUser()).appUserId();
  }

  private DriverProfileResponse reviewDriver(long id, DriverVerificationStatus status) {
    DriverProfileResponse result = service.review(id, status.name());
    audit.record(
        "DRIVER_APPLICATION_REVIEW",
        "driver_profile",
        String.valueOf(id),
        "{\"status\":\"" + status.name() + "\"}");
    return result;
  }
}
