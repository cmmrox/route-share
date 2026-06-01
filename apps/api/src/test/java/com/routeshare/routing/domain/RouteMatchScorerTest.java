package com.routeshare.routing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RouteMatchScorerTest {
  private final RouteMatchScorer scorer = new RouteMatchScorer();

  @Test
  void rejectsCandidateWhenPickupIsAfterDropOnDriverRoute() {
    var candidate =
        new RouteMatchCandidate(
            10L, "Colombo", "Kandy", 2, 12_000, 0.80, 0.20, 100, 100, 8_000, 10_000);

    assertThatThrownBy(() -> scorer.score(candidate))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("pickup must be before drop");
  }

  @Test
  void scoresHighOverlapNearPickupAndDropAboveWeakMatches() {
    var strong =
        new RouteMatchCandidate(
            11L, "Colombo", "Kandy", 3, 12_000, 0.10, 0.90, 100, 150, 9_000, 10_000);
    var weak =
        new RouteMatchCandidate(
            12L, "Colombo", "Gampaha", 3, 12_000, 0.10, 0.45, 900, 950, 3_500, 10_000);

    var strongScore = scorer.score(strong);
    var weakScore = scorer.score(weak);

    assertThat(strongScore.score()).isGreaterThan(weakScore.score());
    assertThat(strongScore.explanation()).contains("overlap");
    assertThat(strongScore.overlapPercent()).isEqualTo(90.0);
  }
}
