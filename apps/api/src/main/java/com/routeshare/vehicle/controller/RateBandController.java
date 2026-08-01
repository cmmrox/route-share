package com.routeshare.vehicle.controller;

import com.routeshare.common.security.DriverSelfServiceAccess;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.vehicle.dto.request.ChosenRateRequest;
import com.routeshare.vehicle.dto.request.RateBandReviewRequestCommand;
import com.routeshare.vehicle.dto.response.RateBandResponse;
import com.routeshare.vehicle.dto.response.RateBandReviewRequestResponse;
import com.routeshare.vehicle.dto.response.VehicleClassResponse;
import com.routeshare.vehicle.service.RateBandService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The driver's side of the band: read it, pick a rate inside it, ask for one re-assessment.
 *
 * <p>Behind the self-service gate rather than the driving gate, because a driver whose band is not
 * yet set is precisely the person board D40 is written for — refusing them here would hide the
 * screen that explains their own wait. Setting the band itself is admin-only and lives elsewhere.
 */
@RestController
@RequestMapping("/api/v1/driver")
@DriverSelfServiceAccess
@RequiredArgsConstructor
public class RateBandController {
  private final RateBandService service;

  @GetMapping("/vehicle-classes")
  ApiResponse<List<VehicleClassResponse>> vehicleClasses() {
    return ApiResponse.ok(service.vehicleClasses());
  }

  @GetMapping("/vehicles/{vehicleId}/rate-band")
  ApiResponse<RateBandResponse> band(@PathVariable long vehicleId) {
    return ApiResponse.ok(service.myBand(vehicleId));
  }

  @PutMapping("/vehicles/{vehicleId}/rate-band/chosen-rate")
  ApiResponse<RateBandResponse> chooseRate(
      @PathVariable long vehicleId, @Valid @RequestBody ChosenRateRequest req) {
    return ApiResponse.ok(service.chooseRate(vehicleId, req.ratePerKm()));
  }

  @PostMapping("/vehicles/{vehicleId}/rate-band/review-requests")
  ApiResponse<RateBandReviewRequestResponse> requestReview(
      @PathVariable long vehicleId, @Valid @RequestBody RateBandReviewRequestCommand req) {
    return ApiResponse.ok(service.requestReview(vehicleId, req));
  }

  @GetMapping("/vehicles/{vehicleId}/rate-band/review-requests")
  ApiResponse<List<RateBandReviewRequestResponse>> myReviewRequests(@PathVariable long vehicleId) {
    return ApiResponse.ok(service.myReviewRequests(vehicleId));
  }
}
