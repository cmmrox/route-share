package com.routeshare.admin.controller;

import com.routeshare.admin.dto.AuditActionResponse;
import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.web.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','OPS_ADMIN')")
public class AdminAuditController {
  private final AdminAuditService service;

  public AdminAuditController(AdminAuditService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/admin/audit/actions")
  ApiResponse<List<AuditActionResponse>> actions(
      @RequestParam(name = "limit", defaultValue = "100") int limit) {
    return ApiResponse.ok(service.recent(limit));
  }
}
