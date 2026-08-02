package com.routeshare.penalty.controller;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.penalty.dto.request.PenaltyDisputeDecisionRequest;
import com.routeshare.penalty.dto.response.PenaltyDisputeResponse;
import com.routeshare.penalty.dto.response.PenaltyResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deciding a dispute moves money, so it sits with the money roles rather than with anyone holding
 * ADMIN. A user cannot decide their own case at all: {@code decide} is reachable only here, and
 * nothing on the passenger or driver surface can waive or reverse a fee.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminPenaltyController {

  private final PenaltyService penalties;
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;

  @GetMapping("/penalties")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','FINANCE_ADMIN','OPS_ADMIN')")
  ApiResponse<List<PenaltyResponse>> list(
      @RequestParam(required = false) String kind, @RequestParam(required = false) String status) {
    return ApiResponse.ok(penalties.adminSearch(kind, status));
  }

  @GetMapping("/penalty-disputes")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','FINANCE_ADMIN','OPS_ADMIN')")
  ApiResponse<List<PenaltyDisputeResponse>> disputes(
      @RequestParam(required = false) String status) {
    return ApiResponse.ok(penalties.adminDisputes(status));
  }

  @PostMapping("/penalty-disputes/{disputeId}/decide")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','FINANCE_ADMIN')")
  ApiResponse<PenaltyDisputeResponse> decide(
      @PathVariable long disputeId, @Valid @RequestBody PenaltyDisputeDecisionRequest request) {
    long adminAppUserId = identityFacade.upsertFromToken(current.requireCurrentUser()).appUserId();
    return ApiResponse.ok(penalties.decide(disputeId, adminAppUserId, request));
  }
}
