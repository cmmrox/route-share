package com.routeshare.location.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DeadReckonerTest {
  private final DeadReckoner reckoner = new DeadReckoner(20);
  private final Instant matched = Instant.parse("2026-08-04T00:00:00Z");

  @Test
  void extrapolatesToCapThenFailsClosedAndNeverOverrunsRoute() {
    var progress =
        new ProgressState(
            0.99,
            LocationConfidence.MATCHED,
            matched,
            matched,
            20.0,
            90.0,
            6.9,
            79.8,
            null,
            null,
            0);
    assertThat(reckoner.estimate(progress, 1_000, matched.plusSeconds(10)))
        .satisfies(
            estimate -> {
              assertThat(estimate.confidence()).isEqualTo(LocationConfidence.EXTRAPOLATED);
              assertThat(estimate.routeFraction()).isEqualTo(1.0);
            });
    assertThat(reckoner.estimate(progress, 1_000, matched.plusSeconds(21)).confidence())
        .isEqualTo(LocationConfidence.STALE);
  }
}
