package com.routeshare.pricing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** The bands from data.jsx: {@code m >= 95 ? 10 : m >= 75 ? 8 : m >= 45 ? 5 : 2.5}. */
class MatchDiscountTierTest {
  private static final BigDecimal HIGH = new BigDecimal("95");
  private static final BigDecimal MID = new BigDecimal("75");
  private static final BigDecimal LOW = new BigDecimal("45");

  private static MatchDiscountTier tier(String matchPercent) {
    return MatchDiscountTier.of(new BigDecimal(matchPercent), HIGH, MID, LOW);
  }

  @Test
  void eachBandStartsExactlyAtItsThreshold() {
    assertThat(tier("95")).isEqualTo(MatchDiscountTier.HIGH);
    assertThat(tier("75")).isEqualTo(MatchDiscountTier.MID);
    assertThat(tier("45")).isEqualTo(MatchDiscountTier.LOW);
  }

  @Test
  void justBelowAThresholdFallsToTheBandBeneath() {
    assertThat(tier("94.99")).isEqualTo(MatchDiscountTier.MID);
    assertThat(tier("74.99")).isEqualTo(MatchDiscountTier.LOW);
    assertThat(tier("44.99")).isEqualTo(MatchDiscountTier.BASE);
  }

  @Test
  void aFullOverlapIsTheTopBand() {
    assertThat(tier("100")).isEqualTo(MatchDiscountTier.HIGH);
  }

  @Test
  void aMissingMatchIsTheCheapestBandForTheRiderNotTheDearest() {
    // No overlap figure means we cannot claim the driver's road is shared, so the smallest
    // discount applies.
    assertThat(MatchDiscountTier.of(null, HIGH, MID, LOW)).isEqualTo(MatchDiscountTier.BASE);
  }

  @Test
  void thresholdsAreParametersSoPolicyCanMoveThem() {
    assertThat(
            MatchDiscountTier.of(
                new BigDecimal("60"),
                new BigDecimal("80"),
                new BigDecimal("55"),
                new BigDecimal("30")))
        .isEqualTo(MatchDiscountTier.MID);
  }
}
