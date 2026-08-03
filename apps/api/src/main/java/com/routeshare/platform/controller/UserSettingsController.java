package com.routeshare.platform.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.platform.dto.request.UserSettingsRequest;
import com.routeshare.platform.dto.response.*;
import com.routeshare.platform.service.UserSettingsService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserSettingsController {
  private final UserSettingsService settings;

  public UserSettingsController(UserSettingsService settings) {
    this.settings = settings;
  }

  @GetMapping("/api/v1/me/settings")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<UserSettingsResponse> mine() {
    return ApiResponse.ok(settings.mine());
  }

  @PutMapping("/api/v1/me/settings")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<UserSettingsResponse> update(@Valid @RequestBody UserSettingsRequest request) {
    return ApiResponse.ok(settings.update(request));
  }

  @PostMapping("/api/v1/me/data-export")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<AccountRequestResponse> dataExport() {
    return ApiResponse.ok(settings.requestDataExport());
  }

  @PostMapping("/api/v1/me/deletion-request")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<AccountRequestResponse> deletion() {
    return ApiResponse.ok(settings.requestDeletion());
  }

  @GetMapping("/api/v1/admin/account-requests")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','OPS_ADMIN','SUPPORT_AGENT')")
  ApiResponse<List<AccountRequestResponse>> accountRequests() {
    return ApiResponse.ok(settings.listAccountRequests());
  }
}
