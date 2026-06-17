package com.routeshare.payment.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CommissionPropertiesTest {

  @Test
  void appliesConfiguredRateWithTwoDecimalRounding() {
    var props = new CommissionProperties(new BigDecimal("0.15"));
    assertThat(props.commissionOn(new BigDecimal("1000.00"))).isEqualByComparingTo("150.00");
    assertThat(props.commissionOn(new BigDecimal("333.33"))).isEqualByComparingTo("50.00");
  }

  @Test
  void fallsBackToTenPercentForInvalidRate() {
    assertThat(new CommissionProperties(null).defaultRate()).isEqualByComparingTo("0.10");
    assertThat(new CommissionProperties(new BigDecimal("-1")).defaultRate())
        .isEqualByComparingTo("0.10");
    assertThat(new CommissionProperties(new BigDecimal("2")).defaultRate())
        .isEqualByComparingTo("0.10");
  }
}
