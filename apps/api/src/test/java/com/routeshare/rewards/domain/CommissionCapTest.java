package com.routeshare.rewards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CommissionCapTest {
  @Test
  void promotionCostIsCappedAndShortfallIsVisible() {
    var result =
        ReferralPolicy.accrue(new BigDecimal("5000"), new BigDecimal("20"), new BigDecimal("400"));
    assertThat(result.requested()).isEqualByComparingTo("1000.00");
    assertThat(result.credited()).isEqualByComparingTo("400.00");
    assertThat(result.shortfall()).isEqualByComparingTo("600.00");
  }
}
