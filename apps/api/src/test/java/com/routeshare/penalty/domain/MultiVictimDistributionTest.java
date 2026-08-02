package com.routeshare.penalty.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D31: a driver's penalty is shared "between them as ride credit".
 *
 * <p>Two things must both be true of that sharing, and neither is automatic. The parts must total
 * the victim half exactly — a dropped remainder destroys money and an added one creates it — and
 * the allocation must not depend on which order the rows came back in, or the same penalty would
 * pay different people differently on a re-run.
 */
class MultiVictimDistributionTest {

  private static BigDecimal money(String value) {
    return new BigDecimal(value).setScale(2);
  }

  @Test
  @DisplayName("D31: 43 across Dinuka and Tharindu is 22 and 21, the remainder to the first")
  void oddHalfGivesTheRemainderToTheFirstBooking() {
    List<BigDecimal> amounts = PenaltyPolicy.distribute(money("43"), 2);
    assertThat(amounts).containsExactly(money("22"), money("21"));
  }

  @Test
  @DisplayName("An even half divides cleanly")
  void evenHalfDividesCleanly() {
    assertThat(PenaltyPolicy.distribute(money("44"), 2)).containsExactly(money("22"), money("22"));
  }

  @Test
  @DisplayName("A single victim takes the whole half")
  void singleVictimTakesEverything() {
    assertThat(PenaltyPolicy.distribute(money("25"), 1)).containsExactly(money("25"));
  }

  @Test
  @DisplayName("The parts total the victim half exactly, for every half and every victim count")
  void partsAlwaysTotalTheVictimShare() {
    for (long half = 0; half <= 2_000; half++) {
      BigDecimal victimShare = BigDecimal.valueOf(half).setScale(2);
      for (int victims = 1; victims <= 6; victims++) {
        List<BigDecimal> amounts = PenaltyPolicy.distribute(victimShare, victims);
        assertThat(amounts).hasSize(victims);
        assertThat(amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add))
            .isEqualByComparingTo(victimShare);
        assertThat(amounts).allSatisfy(amount -> assertThat(amount.signum()).isNotNegative());
      }
    }
  }

  @Test
  @DisplayName("Nobody to pay means nothing is allocated, rather than money left dangling")
  void noVictimsAllocatesNothing() {
    assertThat(PenaltyPolicy.distribute(money("43"), 0)).isEmpty();
  }

  @Test
  @DisplayName("Every part is a whole rupee — ride credit is not quoted in cents")
  void everyPartIsAWholeRupee() {
    for (BigDecimal amount : PenaltyPolicy.distribute(money("100"), 3)) {
      assertThat(amount.stripTrailingZeros().scale()).isLessThanOrEqualTo(0);
    }
  }
}
