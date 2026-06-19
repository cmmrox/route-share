package com.routeshare.admin.controller;

import com.routeshare.admin.dto.AdminDashboardResponse;
import com.routeshare.admin.service.AdminDashboardService;
import com.routeshare.common.web.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','OPS_ADMIN','FINANCE_ADMIN')")
public class AdminDashboardController {
  private final AdminDashboardService service;

  public AdminDashboardController(AdminDashboardService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/admin/dashboard")
  ApiResponse<AdminDashboardResponse> dashboard() {
    return ApiResponse.ok(service.summary());
  }

  @GetMapping("/api/v1/admin/reports/summary")
  ApiResponse<AdminDashboardResponse> reportSummary() {
    return ApiResponse.ok(service.summary());
  }
}
