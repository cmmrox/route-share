package com.routeshare.reliability.facade.impl;

import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.dto.response.EarlyDropAllowanceResponse;
import com.routeshare.reliability.facade.ReliabilityFacade;
import com.routeshare.reliability.service.EarlyDropAllowanceService;
import com.routeshare.reliability.service.ReliabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReliabilityFacadeImpl implements ReliabilityFacade {

  private final EarlyDropAllowanceService earlyDrops;
  private final ReliabilityService reliability;
  private final PolicySettingService policy;

  @Override
  public EarlyDropAllowanceResponse earlyDropAllowance(long appUserId) {
    return earlyDrops.allowance(appUserId);
  }

  @Override
  public boolean consumeEarlyDropAllowance(long appUserId, Long bookingId, Long tripId) {
    return earlyDrops.consume(appUserId, bookingId, tripId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean prepayRequired(long appUserId) {
    int threshold = policy.integer(PolicyKey.PAX_PREPAY_NO_SHOW_THRESHOLD);
    return reliability
            .counter(appUserId, ReliabilityRole.PASSENGER, reliability.currentPeriod())
            .getNoShows()
        >= threshold;
  }
}
