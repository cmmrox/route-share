package com.routeshare.routing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RouteSchedulePolicyTest {
  private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);
  private final RouteSchedulePolicy policy = new RouteSchedulePolicy(clock);

  @Test
  void acceptsFutureOneTimeDepartureAndGeneratesOneOccurrence() {
    var occurrences = policy.generateOneTimeOccurrences(Instant.parse("2026-06-01T11:00:00Z"));

    assertThat(occurrences).containsExactly(Instant.parse("2026-06-01T11:00:00Z"));
  }

  @Test
  void rejectsPastDepartureTimes() {
    assertThatThrownBy(
            () -> policy.generateOneTimeOccurrences(Instant.parse("2026-06-01T09:59:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("future");
  }
}
