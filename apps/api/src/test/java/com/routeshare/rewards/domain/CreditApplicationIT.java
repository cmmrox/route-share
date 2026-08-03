package com.routeshare.rewards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CreditApplicationIT {
  @Test
  void creditNeverExceedsFareOrCreatesANegativePayable() {
    Random samples = new Random(11);
    for (int i = 0; i < 10_000; i++) {
      BigDecimal balance = BigDecimal.valueOf(samples.nextInt(50_000), 2);
      BigDecimal fare = BigDecimal.valueOf(samples.nextInt(20_000), 2);
      BigDecimal credit = ReferralPolicy.rideCredit(balance, fare);
      assertThat(credit).isBetween(BigDecimal.ZERO.setScale(2), fare.setScale(2));
      assertThat(fare.subtract(credit)).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }
  }
}
