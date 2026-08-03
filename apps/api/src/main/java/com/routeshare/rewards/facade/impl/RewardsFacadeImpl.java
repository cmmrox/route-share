package com.routeshare.rewards.facade.impl;

import com.routeshare.rewards.facade.RewardsFacade;
import com.routeshare.rewards.service.RewardsService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RewardsFacadeImpl implements RewardsFacade {
  private final RewardsService rewards;

  @Override
  public void ensureReferralCode(long appUserId, String displayName) {
    rewards.ensureReferralCode(appUserId, displayName);
  }

  @Override
  public void claimAtSignup(
      long appUserId, String phone, String displayName, String code, String deviceId) {
    rewards.claimAtSignup(appUserId, phone, displayName, code, deviceId);
  }

  @Override
  public BigDecimal applyRideCredit(
      long appUserId, long bookingId, BigDecimal fare, Boolean useRewardsCredit) {
    return rewards.applyRideCredit(appUserId, bookingId, fare, useRewardsCredit);
  }

  @Override
  public BigDecimal releaseRideCredit(long appUserId, long bookingId) {
    return rewards.releaseRideCredit(appUserId, bookingId);
  }

  @Override
  public void accrueCompletedTrip(long tripId) {
    rewards.accrueCompletedTrip(tripId);
  }

  @Override
  public String creditCompensation(
      long appUserId, BigDecimal amount, String reference, String description) {
    return rewards.creditCompensation(appUserId, amount, reference, description);
  }

  @Override
  public BigDecimal balance(long appUserId) {
    return rewards.balance(appUserId);
  }
}
