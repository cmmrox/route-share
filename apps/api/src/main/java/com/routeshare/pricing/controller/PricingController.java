package com.routeshare.pricing.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.pricing.domain.FareBreakdown;
import com.routeshare.pricing.domain.FareCalculator;
import com.routeshare.pricing.dto.request.FareEstimateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pricing")
public class PricingController {
  private final FareCalculator calculator = FareCalculator.defaultSriLankaCalculator();

  @PostMapping("/estimate")
  ApiResponse<FareBreakdown> estimate(@Valid @RequestBody FareEstimateRequest req) {
    return ApiResponse.ok(calculator.estimate(req.distanceMeters()));
  }
}
