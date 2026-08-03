package com.routeshare.location.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LoopRouteDisambiguationTest {
  @Test
  void crossingSelectsCandidateNearestToAndNotBehindPriorProgress() {
    var selected =
        new RouteProjector()
            .selectCandidate(
                List.of(new RouteProjection(0.25, 2, 750), new RouteProjection(0.75, 3, 250)),
                0.70);
    assertThat(selected.fraction()).isEqualTo(0.75);
  }
}
