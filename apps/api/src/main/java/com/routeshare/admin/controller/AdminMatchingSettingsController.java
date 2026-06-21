package com.routeshare.admin.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.routing.dto.request.MatchingSettingsRequest;
import com.routeshare.routing.dto.response.MatchingSettingsResponse;
import com.routeshare.routing.service.MatchingSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','OPS_ADMIN')")
public class AdminMatchingSettingsController {
  private final MatchingSettingsService service;

  public AdminMatchingSettingsController(MatchingSettingsService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/admin/matching-settings")
  ApiResponse<MatchingSettingsResponse> get() {
    return ApiResponse.ok(service.get());
  }

  @PutMapping("/api/v1/admin/matching-settings")
  ApiResponse<MatchingSettingsResponse> update(@Valid @RequestBody MatchingSettingsRequest req) {
    return ApiResponse.ok(service.update(req));
  }
}
