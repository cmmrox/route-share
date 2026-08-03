package com.routeshare.rewards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ReferralAccrualTest {
  @Test
  void prototypeDriverAndRiderFiguresRoundToWholeRupees() {
    assertThat(
            ReferralPolicy.accrue(
                    new BigDecimal("1240"), new BigDecimal("2"), new BigDecimal("200"))
                .credited())
        .isEqualByComparingTo("25.00");
    assertThat(
            ReferralPolicy.accrue(new BigDecimal("290"), new BigDecimal("1"), new BigDecimal("29"))
                .credited())
        .isEqualByComparingTo("3.00");
  }
}
