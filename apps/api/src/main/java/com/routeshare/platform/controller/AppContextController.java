package com.routeshare.platform.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.platform.dto.response.AppContextResponse;
import com.routeshare.platform.service.AppContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
}
