package com.routeshare.reliability.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.reliability.dto.response.DriverReliabilityResponse;
import com.routeshare.reliability.dto.response.EarlyDropAllowanceResponse;
import com.routeshare.reliability.dto.response.PassengerReliabilityResponse;
import com.routeshare.reliability.service.EarlyDropAllowanceService;
import com.routeshare.reliability.service.ReliabilityPanelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * D28, P39 and P16. Every one of these reads the caller's own record and takes no identifier:
 * another person's reliability is not something any of these screens can ask for.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReliabilityController {

  private final ReliabilityPanelService panels;
  private final EarlyDropAllowanceService earlyDrops;
  private final com.routeshare.common.security.CurrentUserProvider current;
  private final com.routeshare.identity.facade.IdentityFacade identityFacade;

  /** D28, and D34's evidence. */
  @GetMapping("/driver/reliability")
  @PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
  ApiResponse<DriverReliabilityResponse> driver() {
    return ApiResponse.ok(panels.driverPanel());
  }

  /** P39. */
  @GetMapping("/passenger/reliability")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<PassengerReliabilityResponse> passenger() {
    return ApiResponse.ok(panels.passengerPanel());
  }

  /** P16: what she has left before an early drop stops being repriced. */
  @GetMapping("/passenger/early-drop-allowance")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<EarlyDropAllowanceResponse> earlyDropAllowance() {
    long appUserId = identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
    return ApiResponse.ok(earlyDrops.allowance(appUserId));
  }
}
