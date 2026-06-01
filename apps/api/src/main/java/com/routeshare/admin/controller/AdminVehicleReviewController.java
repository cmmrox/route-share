package com.routeshare.admin.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.vehicle.dto.request.VehicleReviewRequest;
import com.routeshare.vehicle.dto.response.VehicleResponse;
import com.routeshare.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/vehicles")
@PreAuthorize("hasAnyRole('ADMIN','VERIFICATION_AGENT','SUPER_ADMIN')")
public class AdminVehicleReviewController {
  private final VehicleService service;

  public AdminVehicleReviewController(VehicleService service) {
    this.service = service;
  }

  @PostMapping("/{id}/review")
  ApiResponse<VehicleResponse> review(
      @PathVariable long id, @Valid @RequestBody VehicleReviewRequest req) {
    return ApiResponse.ok(service.review(id, req.status()));
  }
}
