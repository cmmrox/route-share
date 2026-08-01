package com.routeshare.admin.controller;

import com.routeshare.admin.service.AdminAuditService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.platform.dto.request.PolicySettingUpdateRequest;
import com.routeshare.platform.dto.response.PolicySettingResponse;
import com.routeshare.platform.service.PolicySettingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The runtime policy surface (decision D1): commission, discount tiers, penalty percentages,
 * waiting times, payout floors.
 *
 * <p>Restricted to the roles that carry money authority. Changing the commission is changing what
 * every driver earns on their next trip, which is not a support action — and every write is audited
 * with its old and new value on top of the history table the service keeps.
 */
@RestController
@RequestMapping("/api/v1/admin/policy-settings")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','FINANCE_ADMIN')")
@RequiredArgsConstructor
public class AdminPolicySettingController {
  private final PolicySettingService policy;
  private final AdminAuditService audit;
  private final CurrentUserProvider currentUsers;
  private final IdentityFacade identity;

  @GetMapping
  ApiResponse<List<PolicySettingResponse>> list() {
    return ApiResponse.ok(policy.all());
  }

  @PutMapping("/{policyKey}")
  ApiResponse<PolicySettingResponse> update(
      @PathVariable String policyKey, @Valid @RequestBody PolicySettingUpdateRequest req) {
    PolicySettingResponse before =
        policy.all().stream()
            .filter(setting -> setting.policyKey().equalsIgnoreCase(policyKey))
            .findFirst()
            .orElse(null);
    var after = policy.update(policyKey, req.value(), currentAdminAppUserId());
    audit.record(
        "POLICY_SETTING_UPDATED",
        "POLICY_SETTING",
        after.policyKey(),
        "{\"from\":\"%s\",\"to\":\"%s\"}"
            .formatted(before == null ? "" : before.value(), after.value()));
    return ApiResponse.ok(after);
  }

  private long currentAdminAppUserId() {
    return identity.upsertFromToken(currentUsers.requireCurrentUser()).appUserId();
  }
}
