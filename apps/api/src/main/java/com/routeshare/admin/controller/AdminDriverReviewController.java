package com.routeshare.admin.controller;

import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.driver.domain.DriverVerificationStatus;
import com.routeshare.driver.dto.response.DriverProfileResponse;
import com.routeshare.driver.service.DriverService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','VERIFICATION_AGENT','SUPER_ADMIN')")
public class AdminDriverReviewController {
  private final DriverService service;
  private final AdminAuditService audit;

  public AdminDriverReviewController(DriverService service, AdminAuditService audit) {
    this.service = service;
    this.audit = audit;
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
