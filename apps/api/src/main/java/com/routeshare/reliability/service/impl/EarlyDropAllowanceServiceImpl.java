package com.routeshare.reliability.service.impl;

import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.dto.response.EarlyDropAllowanceResponse;
import com.routeshare.reliability.service.EarlyDropAllowanceService;
import com.routeshare.reliability.service.ReliabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EarlyDropAllowanceServiceImpl implements EarlyDropAllowanceService {

  private final ReliabilityService reliability;
  private final PolicySettingService policy;

  @Override
  @Transactional
  public EarlyDropAllowanceResponse allowance(long appUserId) {
    var period = reliability.currentPeriod();
    int allowance = policy.integer(PolicyKey.EARLY_DROP_ADJUSTED_PER_MONTH);
    int used =
        reliability.counter(appUserId, ReliabilityRole.PASSENGER, period).getEarlyDropsAdjusted();
    int remaining = Math.max(0, allowance - used);
    return new EarlyDropAllowanceResponse(period, used, allowance, remaining, remaining > 0);
  }

  @Override
  @Transactional
  public boolean consume(long appUserId, Long bookingId, Long tripId) {
    var period = reliability.currentPeriod();
    int allowance = policy.integer(PolicyKey.EARLY_DROP_ADJUSTED_PER_MONTH);
    int used =
        reliability.counter(appUserId, ReliabilityRole.PASSENGER, period).getEarlyDropsAdjusted();
    if (used >= allowance) {
      return false;
    }
    // Only an adjusted drop is recorded. Counting the unadjusted ones too would make the month's
    // allowance run out faster than the rule the rider was shown.
    reliability.record(
        appUserId,
        ReliabilityRole.PASSENGER,
        ReliabilityEventType.EARLY_DROP_ADJUSTED,
        bookingId,
        tripId,
        null);
    return true;
  }
}
