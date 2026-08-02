package com.routeshare.penalty.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The one property that must never fail: the halves re-add to the fee.
 *
 * <p>A split that drifts by a rupee does not look like a bug. It looks like a ledger that is very
 * slightly wrong, once per penalty, for as long as nobody adds up a month of them — which is why
 * this is checked across the whole range rather than at a handful of chosen values, and why the
 * database asserts it too.
 */
class PenaltySplitPropertyTest {

  @Test
  @DisplayName("victimShare + platformShare = feeAmount for every fee from 0 to 1,000,000")
  void halvesAlwaysReAddToTheWhole() {
    for (long fee = 0; fee <= 1_000_000; fee++) {
      BigDecimal amount = BigDecimal.valueOf(fee).setScale(2);
      PenaltySplit split = PenaltyPolicy.split(amount, new BigDecimal("50"));
      if (split.victimShare().add(split.platformShare()).compareTo(amount) != 0) {
        throw new AssertionError(
            "split of " + amount + " gave " + split.victimShare() + " + " + split.platformShare());
      }
    }
  }

  @Test
  @DisplayName("The property holds at any victim percentage, not only at fifty")
  void halvesReAddAtEveryPercentage() {
    for (int percent = 0; percent <= 100; percent++) {
      for (long fee = 0; fee <= 500; fee++) {
        BigDecimal amount = BigDecimal.valueOf(fee).setScale(2);
        PenaltySplit split = PenaltyPolicy.split(amount, BigDecimal.valueOf(percent));
        assertThat(split.victimShare().add(split.platformShare())).isEqualByComparingTo(amount);
        assertThat(split.victimShare().signum()).isGreaterThanOrEqualTo(0);
        assertThat(split.platformShare().signum()).isGreaterThanOrEqualTo(0);
      }
    }
  }

  @Test
  @DisplayName("A victim percentage above 100 cannot pay out more than the fee")
  void theVictimNeverReceivesMoreThanWasCharged() {
    PenaltySplit split = PenaltyPolicy.split(new BigDecimal("49.00"), new BigDecimal("140"));
    assertThat(split.victimShare()).isEqualByComparingTo(new BigDecimal("49.00"));
    assertThat(split.platformShare()).isEqualByComparingTo(BigDecimal.ZERO);
  }
}
