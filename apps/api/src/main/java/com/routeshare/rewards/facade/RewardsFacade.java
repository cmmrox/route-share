package com.routeshare.rewards.facade;

import java.math.BigDecimal;

public interface RewardsFacade {
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
