package com.routeshare.rewards.scheduling;

import com.routeshare.rewards.service.RewardsService;
import com.routeshare.scheduling.domain.ScheduledJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReferralWindowExpiryJob implements ScheduledJob {
  private final RewardsService rewards;

  @Override
  public String name() {
    return "referral-window-expiry";
  }

  @Override
  public int run() {
    return rewards.expireReferralWindows();
  }
}
