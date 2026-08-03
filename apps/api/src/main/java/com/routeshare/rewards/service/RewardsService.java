package com.routeshare.rewards.service;

import com.routeshare.rewards.dto.request.AutoApplyRequest;
import com.routeshare.rewards.dto.request.ReferralClaimRequest;
import com.routeshare.rewards.dto.response.ReferralResponse;
import com.routeshare.rewards.dto.response.RewardsResponse;
import com.routeshare.rewards.dto.response.WithdrawalResponse;
import java.math.BigDecimal;
import java.util.List;

public interface RewardsService {
  ReferralResponse referral(String deviceId);

  ReferralResponse claim(ReferralClaimRequest request, String deviceId);

  RewardsResponse rewards();

  RewardsResponse setAutoApply(AutoApplyRequest request);

  WithdrawalResponse requestWithdrawal();

  List<WithdrawalResponse> withdrawals();

  int expireReferralWindows();

  void ensureReferralCode(long appUserId, String displayName);

  void claimAtSignup(
      long appUserId, String phone, String displayName, String code, String deviceId);

  BigDecimal applyRideCredit(
      long appUserId, long bookingId, BigDecimal fare, Boolean useRewardsCredit);

  BigDecimal releaseRideCredit(long appUserId, long bookingId);

  void accrueCompletedTrip(long tripId);

  String creditCompensation(
      long appUserId, BigDecimal amount, String reference, String description);

  BigDecimal balance(long appUserId);
}
