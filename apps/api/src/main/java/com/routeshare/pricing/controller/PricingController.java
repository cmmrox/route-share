package com.routeshare.pricing.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.maps.service.RouteMetricsPort;
import com.routeshare.pricing.domain.FareBreakdown;
import com.routeshare.pricing.domain.FareCalculator;
import com.routeshare.pricing.dto.request.FareEstimateRequest;
import com.routeshare.pricing.dto.request.RouteFareEstimateRequest;
import com.routeshare.pricing.dto.response.RouteFareResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pricing")
public class PricingController {
  private final FareCalculator calculator = FareCalculator.defaultSriLankaCalculator();
  private final RouteMetricsPort routeMetrics;

  public PricingController(RouteMetricsPort routeMetrics) {
    this.routeMetrics = routeMetrics;
  }

  @PostMapping("/estimate")
  ApiResponse<FareBreakdown> estimate(@Valid @RequestBody FareEstimateRequest req) {
    return ApiResponse.ok(calculator.estimate(req.distanceMeters()));
  }

  /**
   * Server-side fare: resolves distance + duration from coordinates, then prices
   * base+distance+time.
   */
  @PostMapping("/estimate-by-route")
  ApiResponse<RouteFareResponse> estimateByRoute(@Valid @RequestBody RouteFareEstimateRequest req) {
    var metrics =
        routeMetrics.distanceAndDuration(
            req.pickupLat(), req.pickupLng(), req.dropoffLat(), req.dropoffLng());
    var fare = calculator.estimate(metrics.distanceMeters(), metrics.durationSeconds());
    return ApiResponse.ok(
        new RouteFareResponse(
            metrics.distanceMeters(), metrics.durationSeconds(), metrics.source(), fare));
  }
}
