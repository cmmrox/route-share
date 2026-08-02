package com.routeshare.routing.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.routing.dto.request.CoordinateRequest;
import com.routeshare.routing.dto.response.PickupPointResponse;
import com.routeshare.routing.service.PickupPointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * P07/P08/P12 — turning a dropped pin into somewhere a driver can actually find her.
 *
 * <p>Called once, when a booking is being made. Never per search keystroke and never per location
 * ping: the cost model of the whole feature depends on that, and a resolve on every keystroke would
 * be the single largest Google line item in the product.
 */
@RestController
@RequestMapping("/api/v1/passenger/pickup-points")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class PickupPointController {

  private final PickupPointService pickupPoints;

  @PostMapping("/resolve")
  ApiResponse<PickupPointResponse> resolve(@Valid @RequestBody CoordinateRequest coordinate) {
    return ApiResponse.ok(pickupPoints.resolve(coordinate.latitude(), coordinate.longitude()));
  }
}
