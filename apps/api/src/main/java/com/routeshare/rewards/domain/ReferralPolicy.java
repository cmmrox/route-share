package com.routeshare.rewards.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public final class ReferralPolicy {
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private ReferralPolicy() {}

  public static Accrual accrue(
      BigDecimal participantAmount, BigDecimal ratePct, BigDecimal commissionAvailable) {
    BigDecimal requested =
        participantAmount.multiply(ratePct).divide(HUNDRED, 0, RoundingMode.HALF_UP).setScale(2);
    BigDecimal commission = money(commissionAvailable).max(BigDecimal.ZERO.setScale(2));
    BigDecimal credited = requested.min(commission);
    return new Accrual(requested, credited, requested.subtract(credited));
  }

  public static BigDecimal rideCredit(BigDecimal balance, BigDecimal fare) {
    return money(balance).max(BigDecimal.ZERO.setScale(2)).min(money(fare).max(BigDecimal.ZERO));
  }

  public static boolean edgeCanAccrue(
      String status, Instant expiresAt, int tripsCounted, int maxTrips, Instant now) {
    return "ACTIVE".equals(status)
        && now.isBefore(expiresAt)
        && tripsCounted >= 0
        && tripsCounted < maxTrips;
  }

  public static boolean selfReferral(
      long ownerId, long claimantId, boolean sameVerifiedPhone, boolean sameKnownDevice) {
    return ownerId == claimantId || sameVerifiedPhone || sameKnownDevice;
  }

  public static boolean canWithdraw(BigDecimal balance, BigDecimal floor) {
    return money(balance).compareTo(money(floor)) >= 0;
  }

  public static String accrualKey(long edgeId, long bookingId) {
    return "referral:" + edgeId + ":booking:" + bookingId;
  }

  private static BigDecimal money(BigDecimal amount) {
    return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
  }

  public record Accrual(BigDecimal requested, BigDecimal credited, BigDecimal shortfall) {}
}
