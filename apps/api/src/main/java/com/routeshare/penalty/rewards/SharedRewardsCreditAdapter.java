package com.routeshare.penalty.rewards;

import com.routeshare.rewards.facade.RewardsFacade;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Bridges slice 06's narrow credit port to the shared rewards balance introduced in slice 11. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SharedRewardsCreditAdapter implements RewardsCreditPort {

  private final RewardsFacade rewards;

  @Override
  public String credit(long appUserId, BigDecimal amount, String reference, String description) {
    rewards.creditCompensation(appUserId, amount, reference, description);
    log.info(
        "rewards credit recorded appUserId={} amount={} reference={}",
        appUserId,
        amount,
        reference);
    return reference;
  }
}
