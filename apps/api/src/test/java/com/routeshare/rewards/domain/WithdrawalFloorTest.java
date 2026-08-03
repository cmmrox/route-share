package com.routeshare.rewards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WithdrawalFloorTest {
  @Test
  void exactlyTheFloorQualifiesButOneCentBelowDoesNot() {
    BigDecimal floor = new BigDecimal("1000");
    assertThat(ReferralPolicy.canWithdraw(new BigDecimal("999.99"), floor)).isFalse();
    assertThat(ReferralPolicy.canWithdraw(new BigDecimal("1000.00"), floor)).isTrue();
    assertThat(ReferralPolicy.canWithdraw(new BigDecimal("1400"), floor)).isTrue();
  }
}
