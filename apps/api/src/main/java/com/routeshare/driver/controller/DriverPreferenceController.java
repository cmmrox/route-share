package com.routeshare.driver.controller;

import com.routeshare.common.security.DriverSelfServiceAccess;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.driver.dto.request.DrivingPreferenceRequest;
import com.routeshare.driver.dto.response.DrivingPreferenceResponse;
import com.routeshare.driver.dto.response.EligibilityImpactResponse;
import com.routeshare.driver.service.DrivingPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * D35.
 *
 * <p>Behind the weaker self-service gate rather than {@code @DriverAccess}: a deactivated driver
 * still has settings, and a screen that refuses to show a driver what his own account says would
 * strand exactly the person trying to put it right.
 */
@RestController
@RequestMapping("/api/v1/driver/preferences")
@RequiredArgsConstructor
public class DriverPreferenceController {

  private final DrivingPreferenceService preferences;

  @GetMapping
  @DriverSelfServiceAccess
  ApiResponse<DrivingPreferenceResponse> mine() {
    return ApiResponse.ok(preferences.mine());
  }

  @PutMapping
  @DriverSelfServiceAccess
  ApiResponse<DrivingPreferenceResponse> update(
      @Valid @RequestBody DrivingPreferenceRequest request) {
    return ApiResponse.ok(preferences.update(request));
  }

  /** D35's cost line. */
  @GetMapping("/eligibility-impact")
  @DriverSelfServiceAccess
  ApiResponse<EligibilityImpactResponse> eligibilityImpact() {
    return ApiResponse.ok(preferences.eligibilityImpact());
  }
}
