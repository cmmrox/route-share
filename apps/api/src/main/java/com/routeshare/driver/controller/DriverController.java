package com.routeshare.driver.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.driver.dto.request.*;
import com.routeshare.driver.dto.response.*;
import com.routeshare.driver.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/driver")
public class DriverController {
  private final DriverService service;

  public DriverController(DriverService service) {
    this.service = service;
  }

  @PostMapping("/application")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<DriverProfileResponse> apply(@Valid @RequestBody DriverApplicationRequest req) {
    return ApiResponse.ok(service.apply(req));
  }

  @GetMapping("/profile")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<DriverProfileResponse> mine() {
    return ApiResponse.ok(
        service
            .mine()
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found")));
  }
}
