package com.routeshare.location.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RouteProjectorTest {
  @Test
  void choosesNearestProjectionWhenThereIsNoHistory() {
    var projector = new RouteProjector();
    assertThat(
            projector
                .selectCandidate(
                    List.of(new RouteProjection(0.2, 20, 800), new RouteProjection(0.4, 5, 600)),
                    null)
                .fraction())
        .isEqualTo(0.4);
  }
}
