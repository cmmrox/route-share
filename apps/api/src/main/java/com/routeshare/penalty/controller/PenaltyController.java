package com.routeshare.penalty.controller;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.penalty.dto.request.PenaltyDisputeRequest;
import com.routeshare.penalty.dto.response.DuesResponse;
import com.routeshare.penalty.dto.response.PenaltyResponse;
import com.routeshare.penalty.service.DuesService;
import com.routeshare.penalty.service.PenaltyService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * P25, P26, P27, D21, D30, D31, D41.
 *
 * <p>None of these takes another person's identifier. A penalty list is derived from the caller —
 * both directions, since one account both rides and drives, and the same person can be charged by
 * one penalty and paid by another in the same week.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PenaltyController {

  private final PenaltyService penalties;
  private final DuesService dues;
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;

  /** P25 and P25b. */
  @GetMapping("/passenger/dues")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<DuesResponse> dues() {
    return ApiResponse.ok(dues.dues(appUserId()));
  }

  @GetMapping("/passenger/penalties")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<List<PenaltyResponse>> passengerPenalties() {
    return ApiResponse.ok(penalties.listForUser(appUserId()));
  }

  /** D26's two directions: what he was charged, and what somebody else's penalty paid him. */
  @GetMapping("/driver/penalties")
  @PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
  ApiResponse<List<PenaltyResponse>> driverPenalties() {
    return ApiResponse.ok(penalties.listForUser(appUserId()));
  }

  @PostMapping("/passenger/penalties/{penaltyId}/dispute")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<PenaltyResponse> disputeAsPassenger(
      @PathVariable long penaltyId, @Valid @RequestBody PenaltyDisputeRequest request) {
    return ApiResponse.ok(penalties.dispute(penaltyId, appUserId(), request));
  }

  @PostMapping("/driver/penalties/{penaltyId}/dispute")
  @PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
  ApiResponse<PenaltyResponse> disputeAsDriver(
      @PathVariable long penaltyId, @Valid @RequestBody PenaltyDisputeRequest request) {
    return ApiResponse.ok(penalties.dispute(penaltyId, appUserId(), request));
  }

  private long appUserId() {
    return identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
  }
}
