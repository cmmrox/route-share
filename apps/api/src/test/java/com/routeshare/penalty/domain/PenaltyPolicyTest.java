package com.routeshare.penalty.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every penalty figure the prototype states, reproduced exactly.
 *
 * <p>These are not illustrative numbers. Each one appears on a screen the passenger or driver reads
 * — P25's LKR 49, D26's -86 and +37, D31's 22 and 21 — and a backend that computes any of them
 * differently makes the screen a lie.
 */
class PenaltyPolicyTest {
  private static final BigDecimal HALF = new BigDecimal("50");

  private static BigDecimal money(String value) {
    return new BigDecimal(value).setScale(2);
  }

  @Test
  @DisplayName("P27 / P25: a no-show on a LKR 197 fare is 49, split 25 to the driver and 24 to us")
  void noShowOnOneNineSeven() {
    BigDecimal fee = PenaltyPolicy.fee(money("197"), new BigDecimal("25"));
    assertThat(fee).isEqualByComparingTo(money("49"));

    PenaltySplit split = PenaltyPolicy.split(fee, HALF);
    // 24.5 rounds up to the victim, and the platform takes what is left — never a second rounding,
    // which would give 25 and 25 and invent a rupee.
    assertThat(split.victimShare()).isEqualByComparingTo(money("25"));
    assertThat(split.platformShare()).isEqualByComparingTo(money("24"));
  }

  @Test
  @DisplayName("P26: cancelling after start on a LKR 267 fare is 53, split 27 and 26")
  void cancelAfterStartOnTwoSixSeven() {
    BigDecimal fee = PenaltyPolicy.fee(money("267"), new BigDecimal("20"));
    assertThat(fee).isEqualByComparingTo(money("53"));

    PenaltySplit split = PenaltyPolicy.split(fee, HALF);
    assertThat(split.victimShare()).isEqualByComparingTo(money("27"));
    assertThat(split.platformShare()).isEqualByComparingTo(money("26"));
  }

  @Test
  @DisplayName("D30 / D31: a late cancellation on LKR 429 expected net is 86, split 43 and 43")
  void driverLateCancellationOnFourTwoNine() {
    BigDecimal fee = PenaltyPolicy.fee(money("429"), new BigDecimal("20"));
    assertThat(fee).isEqualByComparingTo(money("86"));

    PenaltySplit split = PenaltyPolicy.split(fee, HALF);
    assertThat(split.victimShare()).isEqualByComparingTo(money("43"));
    assertThat(split.platformShare()).isEqualByComparingTo(money("43"));
  }

  @Test
  @DisplayName("D41: a driver late to a LKR 251 seat pays 50, and 25 of it reaches her")
  void driverLateOnTwoFiveOneSeatNet() {
    BigDecimal fee = PenaltyPolicy.fee(money("251"), new BigDecimal("20"));
    assertThat(fee).isEqualByComparingTo(money("50"));
    assertThat(PenaltyPolicy.split(fee, HALF).victimShare()).isEqualByComparingTo(money("25"));
  }

  @Test
  @DisplayName("D26: a no-show on a LKR 290 fare pays the driver 37 in compensation")
  void noShowCompensationOnTwoNinety() {
    BigDecimal fee = PenaltyPolicy.fee(money("290"), new BigDecimal("25"));
    assertThat(fee).isEqualByComparingTo(money("73"));

    PenaltySplit split = PenaltyPolicy.split(fee, HALF);
    assertThat(split.victimShare()).isEqualByComparingTo(money("37"));
    assertThat(split.platformShare()).isEqualByComparingTo(money("36"));
  }

  @Test
  @DisplayName("A kind with no fee charges nothing, and its halves are both zero")
  void zeroFeeKindsChargeNothing() {
    BigDecimal fee = PenaltyPolicy.fee(money("429"), BigDecimal.ZERO);
    assertThat(fee).isEqualByComparingTo(money("0"));

    PenaltySplit split = PenaltyPolicy.split(fee, HALF);
    assertThat(split.victimShare()).isEqualByComparingTo(money("0"));
    assertThat(split.platformShare()).isEqualByComparingTo(money("0"));
  }

  @Test
  @DisplayName("Money is whole rupees: a fee is never a figure nobody can hand over")
  void feesAreWholeRupees() {
    assertThat(PenaltyPolicy.fee(money("197"), new BigDecimal("25")).stripTrailingZeros().scale())
        .isLessThanOrEqualTo(0);
  }

  @Test
  @DisplayName("Every penalty kind names the policy setting that prices it, or carries no fee")
  void kindsArePricedFromPolicyRatherThanConstants() {
    for (PenaltyKind kind : PenaltyKind.values()) {
      if (kind == PenaltyKind.DRIVER_MISSED_START) {
        assertThat(kind.carriesFee()).isFalse();
        assertThat(kind.rateKey()).isNull();
      } else {
        assertThat(kind.carriesFee()).isTrue();
        assertThat(kind.rateKey()).isNotNull();
      }
    }
  }
}
