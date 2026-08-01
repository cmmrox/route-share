package com.routeshare.pricing.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.pricing.dto.request.RouteFareEstimateRequest;
import com.routeshare.pricing.dto.response.RouteFareResponse;
import com.routeshare.pricing.service.PricingQuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fare estimation.
 *
 * <p>{@code POST /pricing/estimate} is gone. It took a distance from the request body, which meant
 * a client could name the number its own fare was computed from — a free-money bug wearing the
 * shape of an API. Every price now derives from server-side geometry and the vehicle's assessed
 * band.
 */
@RestController
@RequestMapping("/api/v1/pricing")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class PricingController {
  private final PricingQuoteService quotes;

  @PostMapping("/estimate-by-route")
  ApiResponse<RouteFareResponse> estimateByRoute(@Valid @RequestBody RouteFareEstimateRequest req) {
    return ApiResponse.ok(quotes.estimateByRoute(req));
  }
}
