package com.routeshare.admin.controller;

import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.passenger.dto.request.VerificationDecisionRequest;
import com.routeshare.passenger.dto.response.PassengerVerificationResponse;
import com.routeshare.passenger.dto.response.VerificationSessionResponse;
import com.routeshare.passenger.service.PassengerVerificationService;
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
 * The human half of P29's camera-only rule.
 *
 * <p>Every decision is audited with the reviewer, the per-step outcomes and the gender written —
 * gender is the one value in the system a reviewer sets on someone else's behalf, and it decides
 * whether she may ride in a women-only car.
 */
@RestController
@RequestMapping("/api/v1/admin/passenger-verifications")
@PreAuthorize("hasAnyRole('ADMIN','VERIFICATION_AGENT','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminPassengerVerificationController {

  private final PassengerVerificationService verification;
  private final AdminAuditService audit;

  @GetMapping("/pending")
  ApiResponse<List<VerificationSessionResponse>> pending() {
    return ApiResponse.ok(verification.pendingForReview());
  }

  @PostMapping("/{sessionId}/decide")
  ApiResponse<PassengerVerificationResponse> decide(
      @PathVariable long sessionId, @Valid @RequestBody VerificationDecisionRequest request) {
    var result = verification.decide(sessionId, request);
    audit.record(
        "PASSENGER_VERIFICATION_DECIDED",
        "verification_session",
        String.valueOf(sessionId),
        "{\"decision\":\""
            + request.decision()
            + "\",\"gender\":\""
            + (request.gender() == null ? "UNSPECIFIED" : request.gender())
            + "\",\"rejectedSteps\":"
            + auditList(request.rejectedSteps())
            + "}");
    return ApiResponse.ok(result);
  }

  private static String auditList(List<String> values) {
    if (values == null || values.isEmpty()) {
      return "[]";
    }
    return values.stream()
        .map(v -> "\"" + v.replace("\"", "") + "\"")
        .collect(java.util.stream.Collectors.joining(",", "[", "]"));
  }
}
