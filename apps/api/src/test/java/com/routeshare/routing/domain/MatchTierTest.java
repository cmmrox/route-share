package com.routeshare.routing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.routeshare.pricing.domain.MatchDiscountTier;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The tier a rider reads and the discount she is charged come from one set of thresholds.
 *
 * <p>The failure this guards against is subtle and would reach production silently: a second copy
 * of 95/75/45 drifting from the first, so P05 groups a trip under "Full route" while the fare
 * applies the 8% band. The rider would be right and the app would be wrong, and nothing would
 * throw.
 */
class MatchTierTest {

  private static final BigDecimal HIGH = new BigDecimal("95");
  private static final BigDecimal MID = new BigDecimal("75");
  private static final BigDecimal LOW = new BigDecimal("45");

  @Test
  @DisplayName("09-8: a full-route match is FULL_ROUTE and the top discount band")
  void fullRoute() {
    assertThat(MatchTier.of(new BigDecimal("100"), HIGH, MID, LOW)).isEqualTo(MatchTier.FULL_ROUTE);
    assertThat(MatchDiscountTier.of(new BigDecimal("100"), HIGH, MID, LOW))
        .isEqualTo(MatchDiscountTier.HIGH);
  }

  @Test
  @DisplayName("09-9: a 74% match is PART_OF_ROUTE and the low band, not the middle one")
  void partOfRoute() {
    assertThat(MatchTier.of(new BigDecimal("74"), HIGH, MID, LOW))
        .isEqualTo(MatchTier.PART_OF_ROUTE);
    assertThat(MatchDiscountTier.of(new BigDecimal("74"), HIGH, MID, LOW))
        .isEqualTo(MatchDiscountTier.LOW);
  }

  @Test
  @DisplayName("every discount band maps to exactly one tier, and every tier is reachable")
  void mappingIsTotalAndOnto() {
    // A default arm here would be a bug hiding: it would silently absorb a new discount band and
    // group it under whatever the fallback happened to be.
    var tiers =
        java.util.Arrays.stream(MatchDiscountTier.values()).map(MatchTier::of).distinct().toList();
    assertThat(tiers).hasSize(MatchDiscountTier.values().length);
    assertThat(tiers).containsExactlyInAnyOrder(MatchTier.values());
  }

  @Test
  @DisplayName("the thresholds are inclusive lower bounds, exactly as the discount bands are")
  void boundariesAgreeWithTheDiscountBands() {
    for (String percent : new String[] {"95", "75", "45", "44.99", "0"}) {
      var value = new BigDecimal(percent);
      assertThat(MatchTier.of(value, HIGH, MID, LOW))
          .as("tier at %s%% must follow the discount band", percent)
          .isEqualTo(MatchTier.of(MatchDiscountTier.of(value, HIGH, MID, LOW)));
    }
  }

  @Test
  @DisplayName("a missing percentage is the lowest tier, never the highest")
  void nullPercentIsShortHop() {
    assertThat(MatchTier.of((BigDecimal) null, HIGH, MID, LOW)).isEqualTo(MatchTier.SHORT_HOP);
  }

  @Test
  @DisplayName("every tier carries the label the screen groups under")
  void tiersAreLabelled() {
    assertThat(MatchTier.FULL_ROUTE.label()).isEqualTo("Full route");
    assertThat(java.util.Arrays.stream(MatchTier.values()).map(MatchTier::label))
        .allSatisfy(label -> assertThat(label).isNotBlank());
  }
}
