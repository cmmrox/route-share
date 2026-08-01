package com.routeshare.pricing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * The engine against the prototype's own fixtures.
 *
 * <p>{@code data.jsx} is the specification of record, and every money figure on nine screens is
 * derived from these numbers. A test that merely agreed with the implementation would prove
 * nothing; these assert the figures the product already states.
 */
class FareEngineTest {
  private static final Instant NOW = Instant.parse("2026-08-01T09:41:00Z");
  private static final BigDecimal RATE = new BigDecimal("50");
  private static final BigDecimal COMMISSION = new BigDecimal("10");

  private static FareQuote quote(
      String meters, int seats, String matchPercent, MatchDiscountTier tier, String discountPct) {
    return FareEngine.quote(
        "LKR",
        new BigDecimal(meters),
        RATE,
        seats,
        new BigDecimal(matchPercent),
        tier,
        new BigDecimal(discountPct),
        COMMISSION,
        null,
        NOW,
        "v-test");
  }

  @Test
  void theFullRouteFareMatchesTheFixture() {
    // data.jsx: NEXT_DRIVE_KM 11.4 at LKR 50/km -> 570.
    FareQuote q = quote("11400", 1, "100", MatchDiscountTier.HIGH, "0");

    assertThat(q.grossFare()).isEqualByComparingTo("570");
  }

  @Test
  void theSeatFareMatchesTheFixtureExactly() {
    // 5.8 km at 50/km on a 92% match: gross 290, discount 23, price 267, driver net 240.
    FareQuote q = quote("5800", 1, "92", MatchDiscountTier.MID, "8");

    assertThat(q.grossFare()).isEqualByComparingTo("290");
    assertThat(q.discountAmount()).isEqualByComparingTo("23");
    assertThat(q.passengerPays()).isEqualByComparingTo("267");
    assertThat(q.commissionAmount()).isEqualByComparingTo("27");
    assertThat(q.driverNet()).isEqualByComparingTo("240");
  }

  @Test
  void commissionComesOutOfTheFareNotOnTopOfIt() {
    FareQuote q = quote("5800", 1, "92", MatchDiscountTier.MID, "8");

    // The old engine added a fee: the passenger paid 267 + 27. This is the whole point of the
    // rewrite — one price, and the platform's cut inside it.
    assertThat(q.passengerPays()).isEqualByComparingTo(q.grossFare().subtract(q.discountAmount()));
    assertThat(q.driverNet().add(q.commissionAmount())).isEqualByComparingTo(q.passengerPays());
  }

  @Test
  void twoSeatsCostExactlyTwiceOneSeat() {
    FareQuote one = quote("5800", 1, "92", MatchDiscountTier.MID, "8");
    FareQuote two = quote("5800", 2, "92", MatchDiscountTier.MID, "8");

    // A rider booking two seats and two friends booking one each must never pay different totals.
    assertThat(two.grossFare()).isEqualByComparingTo(one.grossFare().multiply(new BigDecimal("2")));
  }

  @Test
  void thereIsNoBaseFareAndNoTimeComponent() {
    FareQuote zero = quote("0", 1, "0", MatchDiscountTier.BASE, "2.5");

    // The retired engine charged 250 before the wheels moved.
    assertThat(zero.grossFare()).isEqualByComparingTo("0");
    assertThat(zero.passengerPays()).isEqualByComparingTo("0");
  }

  @Test
  void theMinimumFareFloorsAShortHopAndSaysSo() {
    FareQuote q =
        FareEngine.quote(
            "LKR",
            new BigDecimal("400"),
            RATE,
            1,
            new BigDecimal("30"),
            MatchDiscountTier.BASE,
            new BigDecimal("2.5"),
            COMMISSION,
            new BigDecimal("100"),
            NOW,
            "v-test");

    assertThat(q.minFareApplied()).isTrue();
    assertThat(q.passengerPays()).isEqualByComparingTo("100");
    // The receipt must still add up on screen after the floor lifts the price.
    assertThat(q.grossFare().subtract(q.discountAmount())).isEqualByComparingTo(q.passengerPays());
    assertThat(q.driverNet().add(q.commissionAmount())).isEqualByComparingTo(q.passengerPays());
  }

  @Test
  void theInvariantsHoldAcrossEveryRoundingPath() {
    // Distances chosen to land on .5 rupee boundaries in gross, discount and commission at once.
    for (int meters = 1; meters <= 20_000; meters += 137) {
      for (int seats = 1; seats <= 4; seats++) {
        for (String discount : new String[] {"0", "2.5", "5", "8", "10"}) {
          FareQuote q = quote(String.valueOf(meters), seats, "50", MatchDiscountTier.LOW, discount);
          assertThat(q.driverNet().add(q.commissionAmount()))
              .as("commission splits the fare at %d m, %d seats, %s%%", meters, seats, discount)
              .isEqualByComparingTo(q.passengerPays());
          assertThat(q.grossFare().subtract(q.discountAmount()))
              .as("discount comes off gross at %d m, %d seats, %s%%", meters, seats, discount)
              .isEqualByComparingTo(q.passengerPays());
        }
      }
    }
  }

  @Test
  void everyFigureIsAWholeRupee() {
    FareQuote q = quote("7321", 3, "61", MatchDiscountTier.LOW, "5");

    for (BigDecimal amount :
        new BigDecimal[] {
          q.grossFare(), q.discountAmount(), q.passengerPays(), q.commissionAmount(), q.driverNet()
        }) {
      assertThat(amount.stripTrailingZeros().scale())
          .as("%s should be a whole rupee", amount)
          .isLessThanOrEqualTo(0);
    }
  }

  @Test
  void aNegativeDistanceIsRefusedRatherThanPriced() {
    assertThatThrownBy(() -> quote("-1", 1, "50", MatchDiscountTier.LOW, "5"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aVehicleWithNoRateCannotBePriced() {
    assertThatThrownBy(
            () ->
                FareEngine.quote(
                    "LKR",
                    new BigDecimal("5800"),
                    BigDecimal.ZERO,
                    1,
                    new BigDecimal("92"),
                    MatchDiscountTier.MID,
                    new BigDecimal("8"),
                    COMMISSION,
                    null,
                    NOW,
                    "v"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void zeroSeatsIsNotABooking() {
    assertThatThrownBy(() -> quote("5800", 0, "92", MatchDiscountTier.MID, "8"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
