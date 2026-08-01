package com.routeshare.platform.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.platform.dto.request.ActiveModeRequest;
import com.routeshare.platform.dto.response.ActiveModeResponse;
import com.routeshare.platform.dto.response.AppContextResponse;
import com.routeshare.platform.service.AppContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class AppContextController {
  private final AppContextService service;

  /**
   * The app shell's single read. Takes no identifier: the caller can only ever see their own
   * context.
   */
  @GetMapping("/context")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<AppContextResponse> context() {
    return ApiResponse.ok(service.current());
  }

  /** Remembers the mode the user switched into, so the next cold start lands in the same place. */
  @PutMapping("/active-mode")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<ActiveModeResponse> activeMode(@Valid @RequestBody ActiveModeRequest req) {
    return ApiResponse.ok(service.setActiveMode(req.mode()));
  }
}
