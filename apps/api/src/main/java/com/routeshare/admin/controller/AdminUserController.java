package com.routeshare.admin.controller;

import com.routeshare.admin.dto.AdminActionRequest;
import com.routeshare.admin.dto.AdminUserResponse;
import com.routeshare.admin.dto.UserStatusHistoryResponse;
import com.routeshare.admin.service.AdminUserService;
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
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','OPS_ADMIN')")
public class AdminUserController {
  private final AdminUserService service;

  public AdminUserController(AdminUserService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/admin/users")
  ApiResponse<List<AdminUserResponse>> list(
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ApiResponse.ok(service.list(limit));
  }

  @GetMapping("/api/v1/admin/users/{appUserId}")
  ApiResponse<AdminUserResponse> get(@PathVariable long appUserId) {
    return ApiResponse.ok(service.get(appUserId));
  }

  @PostMapping("/api/v1/admin/users/{appUserId}/suspend")
  ApiResponse<AdminUserResponse> suspend(
      @PathVariable long appUserId, @RequestBody(required = false) AdminActionRequest req) {
    return ApiResponse.ok(service.suspend(appUserId, req == null ? null : req.reason()));
  }

  @PostMapping("/api/v1/admin/users/{appUserId}/activate")
  ApiResponse<AdminUserResponse> activate(
      @PathVariable long appUserId, @RequestBody(required = false) AdminActionRequest req) {
    return ApiResponse.ok(service.activate(appUserId, req == null ? null : req.reason()));
  }

  @GetMapping("/api/v1/admin/users/{appUserId}/status-history")
  ApiResponse<List<UserStatusHistoryResponse>> statusHistory(@PathVariable long appUserId) {
    return ApiResponse.ok(service.statusHistory(appUserId));
  }
}
