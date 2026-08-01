package com.routeshare.driver.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.driver.dto.request.DriverReinstatementRequest;
import com.routeshare.driver.dto.response.DriverReinstatementRequestResponse;
import com.routeshare.driver.service.DriverDeactivationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * D34's way back.
 *
 * <p>Deliberately <em>not</em> behind the driver gate: the only person who needs this endpoint is
 * the driver the gate is refusing. Ownership is still absolute — the caller can only ever see and
 * create requests against their own profile, which is resolved from the token, never from the body.
 */
@RestController
@RequestMapping("/api/v1/driver/reinstatement-requests")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class DriverReinstatementController {
  private final DriverDeactivationService service;

  @PostMapping
  ApiResponse<DriverReinstatementRequestResponse> request(
      @Valid @RequestBody DriverReinstatementRequest req) {
    return ApiResponse.ok(service.requestReinstatement(req.message()));
  }

  @GetMapping
  ApiResponse<List<DriverReinstatementRequestResponse>> mine() {
    return ApiResponse.ok(service.myReinstatementRequests());
  }
}
