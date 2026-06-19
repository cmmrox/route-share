package com.routeshare.admin.controller;

import com.routeshare.admin.dto.AdminActionRequest;
import com.routeshare.admin.dto.AdminSosResponse;
import com.routeshare.admin.service.AdminSafetyService;
import com.routeshare.common.web.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SUPPORT_AGENT','OPS_ADMIN')")
public class AdminSafetyController {
  private final AdminSafetyService service;

  public AdminSafetyController(AdminSafetyService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/admin/safety/sos-events")
  ApiResponse<List<AdminSosResponse>> list(
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ApiResponse.ok(service.list(status, limit));
  }

  @GetMapping("/api/v1/admin/safety/sos-events/{sosEventId}")
  ApiResponse<AdminSosResponse> get(@PathVariable long sosEventId) {
    return ApiResponse.ok(service.get(sosEventId));
  }

  @PostMapping("/api/v1/admin/safety/sos-events/{sosEventId}/resolve")
  ApiResponse<AdminSosResponse> resolve(
      @PathVariable long sosEventId, @RequestBody(required = false) AdminActionRequest req) {
    return ApiResponse.ok(service.resolve(sosEventId, req == null ? null : req.reason()));
  }
}
