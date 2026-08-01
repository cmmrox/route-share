package com.routeshare.vehicle.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Position drives both the driver's screen (D39) and the passenger's explanation (P07), so it is
 * derived in one place — the two must never tell different stories about the same rate.
 */
class RatePositionTest {
  private static final BigDecimal MIN = new BigDecimal("41");
  private static final BigDecimal MAX = new BigDecimal("58");

  @Test
  void theFloorOfTheBandIsTheBottomPosition() {
    assertThat(RatePosition.of(MIN, MIN, MAX)).isEqualTo(RatePosition.MIN);
  }

  @Test
  void theCeilingOfTheBandIsTheTopPosition() {
    assertThat(RatePosition.of(MAX, MIN, MAX)).isEqualTo(RatePosition.MAX);
  }

  @Test
  void theMidpointIsTheMiddlePosition() {
    assertThat(RatePosition.of(new BigDecimal("50"), MIN, MAX)).isEqualTo(RatePosition.MID);
  }

  @Test
  void theBottomThirdIsStillTheBottomPosition() {
    assertThat(RatePosition.of(new BigDecimal("45"), MIN, MAX)).isEqualTo(RatePosition.MIN);
  }

  @Test
  void theTopThirdIsStillTheTopPosition() {
    assertThat(RatePosition.of(new BigDecimal("55"), MIN, MAX)).isEqualTo(RatePosition.MAX);
  }

  @Test
  void aBandWithNoRoomToMoveHasNoPositionToTake() {
    assertThat(RatePosition.of(MIN, MIN, MIN)).isEqualTo(RatePosition.MID);
  }

  @Test
  void anUnchosenRateDoesNotCrashTheScreen() {
    assertThat(RatePosition.of(null, MIN, MAX)).isEqualTo(RatePosition.MID);
  }

  @Test
  void everyPositionCarriesTheCopyBothScreensRender() {
    for (RatePosition position : RatePosition.values()) {
      assertThat(position.label()).isNotBlank();
      assertThat(position.rank()).isNotBlank();
      assertThat(position.demand()).isNotBlank();
    }
  }
}
