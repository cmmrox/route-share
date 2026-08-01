package com.routeshare.admin.controller;

import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.vehicle.dto.request.RateBandAssessmentCommand;
import com.routeshare.vehicle.dto.request.RateBandReviewDecisionCommand;
import com.routeshare.vehicle.dto.response.RateBandResponse;
import com.routeshare.vehicle.dto.response.RateBandReviewRequestResponse;
import com.routeshare.vehicle.service.RateBandService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Band assessment, admin side.
 *
 * <p>Deliberately narrower than the rest of the admin suite: a verification agent may approve a
 * car's papers but may not price it. Setting a band is setting what riders pay, so it stays with
 * the roles that already carry money authority.
 */
@RestController
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','FINANCE_ADMIN')")
@RequiredArgsConstructor
public class AdminRateBandController {
  private final RateBandService service;
  private final AdminAuditService audit;
  private final CurrentUserProvider currentUsers;
  private final IdentityFacade identity;

  @GetMapping("/api/v1/admin/vehicles/{vehicleId}/rate-band")
  ApiResponse<RateBandResponse> band(@PathVariable long vehicleId) {
    return ApiResponse.ok(service.bandFor(vehicleId));
  }

  @PutMapping("/api/v1/admin/vehicles/{vehicleId}/rate-band")
  ApiResponse<RateBandResponse> assess(
      @PathVariable long vehicleId, @Valid @RequestBody RateBandAssessmentCommand req) {
    // Read the band first so the audit row carries before *and* after: a price change with only
    // its outcome recorded cannot be reviewed later.
    RateBandResponse before = service.bandFor(vehicleId);
    RateBandResponse after = service.assess(vehicleId, req, currentAdminAppUserId());
    audit.record(
        "VEHICLE_RATE_BAND_ASSESSED",
        "vehicle",
        String.valueOf(vehicleId),
        "{\"from\":\"%s-%s\",\"to\":\"%s-%s\",\"note\":\"%s\"}"
            .formatted(
                before.band().min(),
                before.band().max(),
                after.band().min(),
                after.band().max(),
                req.note() == null ? "" : req.note()));
    return ApiResponse.ok(after);
  }

  @GetMapping("/api/v1/admin/rate-band-review-requests")
  ApiResponse<List<RateBandReviewRequestResponse>> reviewRequests(
      @RequestParam(required = false, defaultValue = "OPEN") String status) {
    return ApiResponse.ok(service.reviewRequests(status));
  }

  @PostMapping("/api/v1/admin/rate-band-review-requests/{requestId}/decide")
  ApiResponse<RateBandReviewRequestResponse> decide(
      @PathVariable long requestId, @Valid @RequestBody RateBandReviewDecisionCommand req) {
    var result = service.decideReview(requestId, req, currentAdminAppUserId());
    audit.record(
        "VEHICLE_RATE_BAND_REVIEW_DECIDED",
        "vehicle",
        String.valueOf(result.vehicleId()),
        "{\"requestId\":%d,\"decision\":\"%s\",\"note\":\"%s\"}"
            .formatted(requestId, result.status(), req.note() == null ? "" : req.note()));
    return ApiResponse.ok(result);
  }

  private long currentAdminAppUserId() {
    return identity.upsertFromToken(currentUsers.requireCurrentUser()).appUserId();
  }
}
