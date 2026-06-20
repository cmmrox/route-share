package com.routeshare.pricing.domain;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FareCalculatorTest {
  @Test
  void estimatesFareWithBaseDistanceAndPlatformFee() {
    var calc =
        new FareCalculator(
            new BigDecimal("250.00"), new BigDecimal("90.00"), new BigDecimal("0.10"));
    var fare = calc.estimate(10000);
    assertThat(fare.baseFare()).isEqualByComparingTo("250.00");
    assertThat(fare.distanceFare()).isEqualByComparingTo("900.00");
    assertThat(fare.platformFee()).isEqualByComparingTo("115.00");
    assertThat(fare.totalFare()).isEqualByComparingTo("1265.00");
  }

  @Test
  void addsTimeFareFromDuration() {
    var calc =
        new FareCalculator(
            new BigDecimal("250.00"),
            new BigDecimal("90.00"),
            new BigDecimal("5.00"),
            new BigDecimal("0.10"));
    var fare = calc.estimate(10000, 600); // 10 km, 10 min
    assertThat(fare.distanceFare()).isEqualByComparingTo("900.00");
    assertThat(fare.timeFare()).isEqualByComparingTo("50.00");
    // subtotal 1200 -> fee 120 -> total 1320
    assertThat(fare.platformFee()).isEqualByComparingTo("120.00");
    assertThat(fare.totalFare()).isEqualByComparingTo("1320.00");
  }

  @Test
  void rejectsNegativeDistance() {
    var calc = new FareCalculator(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO);
    assertThatThrownBy(() -> calc.estimate(-1)).isInstanceOf(IllegalArgumentException.class);
  }
}
