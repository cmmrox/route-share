package com.routeshare.admin.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.driver.domain.DriverVerificationStatus;
import com.routeshare.driver.dto.response.DriverProfileResponse;
import com.routeshare.driver.service.DriverService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/drivers")
@PreAuthorize("hasAnyRole('ADMIN','VERIFICATION_AGENT','SUPER_ADMIN')")
public class AdminDriverReviewController {
  private final DriverService service;

  public AdminDriverReviewController(DriverService service) {
    this.service = service;
  }

  @PostMapping("/{id}/review")
  ApiResponse<DriverProfileResponse> review(
      @PathVariable long id, @RequestParam DriverVerificationStatus status) {
    return ApiResponse.ok(service.review(id, status.name()));
  }
}
