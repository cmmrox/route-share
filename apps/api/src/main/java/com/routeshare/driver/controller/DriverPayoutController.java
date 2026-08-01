package com.routeshare.driver.controller;

import com.routeshare.common.security.DriverSelfServiceAccess;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.driver.dto.request.PayoutProfileRequest;
import com.routeshare.driver.dto.response.PayoutProfileResponse;
import com.routeshare.driver.service.DriverPayoutService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver/payout-profile")
@DriverSelfServiceAccess
public class DriverPayoutController {
  private final DriverPayoutService service;

  public DriverPayoutController(DriverPayoutService service) {
    this.service = service;
  }

  @GetMapping
  ApiResponse<PayoutProfileResponse> get() {
    return ApiResponse.ok(service.getMine());
  }

  @PutMapping
  ApiResponse<PayoutProfileResponse> update(@Valid @RequestBody PayoutProfileRequest req) {
    return ApiResponse.ok(service.saveMine(req));
  }
}
