package com.routeshare.rewards.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.rewards.dto.request.AutoApplyRequest;
import com.routeshare.rewards.dto.request.ReferralClaimRequest;
import com.routeshare.rewards.dto.response.ReferralResponse;
import com.routeshare.rewards.dto.response.RewardsResponse;
import com.routeshare.rewards.dto.response.WithdrawalResponse;
import com.routeshare.rewards.service.RewardsService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class RewardsController {
  private final RewardsService rewards;

  @GetMapping("/referral")
  ApiResponse<ReferralResponse> referral(
      @RequestHeader(name = "X-Device-Id", required = false) String deviceId) {
    return ApiResponse.ok(rewards.referral(deviceId));
  }

  @PostMapping("/referral/claim")
  ApiResponse<ReferralResponse> claim(
      @Valid @RequestBody ReferralClaimRequest request,
      @RequestHeader(name = "X-Device-Id", required = false) String deviceId) {
    return ApiResponse.ok(rewards.claim(request, deviceId));
  }

  @GetMapping("/rewards")
  ApiResponse<RewardsResponse> rewards() {
    return ApiResponse.ok(rewards.rewards());
  }

  @PutMapping("/rewards/auto-apply")
  ApiResponse<RewardsResponse> autoApply(@Valid @RequestBody AutoApplyRequest request) {
    return ApiResponse.ok(rewards.setAutoApply(request));
  }

  @PostMapping("/rewards/withdrawals")
  ApiResponse<WithdrawalResponse> withdraw() {
    return ApiResponse.ok(rewards.requestWithdrawal());
  }

  @GetMapping("/rewards/withdrawals")
  ApiResponse<List<WithdrawalResponse>> withdrawals() {
    return ApiResponse.ok(rewards.withdrawals());
  }
}
