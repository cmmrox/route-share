package com.routeshare.admin.controller;

import com.routeshare.admin.dto.BroadcastRequest;
import com.routeshare.admin.service.AdminBroadcastService;
import com.routeshare.common.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','OPS_ADMIN')")
public class AdminBroadcastController {
  private final AdminBroadcastService service;

  public AdminBroadcastController(AdminBroadcastService service) {
    this.service = service;
  }

  @PostMapping("/api/v1/admin/notifications/broadcasts")
  ApiResponse<Map<String, Object>> broadcast(@Valid @RequestBody BroadcastRequest req) {
    return ApiResponse.ok(service.broadcast(req));
  }
}
