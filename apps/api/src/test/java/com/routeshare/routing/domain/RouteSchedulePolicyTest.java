package com.routeshare.routing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RouteSchedulePolicyTest {
  // 2026-06-01 is a Monday.
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

  @Test
  void expandsRecurringAcrossSelectedWeekdays() {
    var occ =
        policy.generateRecurringOccurrences(
            Instant.parse("2026-06-01T18:00:00Z"),
            null,
            Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            Instant.parse("2026-06-08T23:59:00Z"),
            null,
            60);

    assertThat(occ)
        .containsExactly(
            Instant.parse("2026-06-01T18:00:00Z"),
            Instant.parse("2026-06-03T18:00:00Z"),
            Instant.parse("2026-06-05T18:00:00Z"),
            Instant.parse("2026-06-08T18:00:00Z"));
  }

  @Test
  void dailyRecurrenceIsCappedByMaxToGenerate() {
    var occ =
        policy.generateRecurringOccurrences(
            Instant.parse("2026-06-01T18:00:00Z"),
            null,
            Set.of(),
            Instant.parse("2026-12-31T00:00:00Z"),
            null,
            3);

    assertThat(occ).hasSize(3);
  }

  @Test
  void skipsOccurrencesUpToAndIncludingAnchorWhenExtending() {
    var occ =
        policy.generateRecurringOccurrences(
            Instant.parse("2026-06-01T18:00:00Z"),
            null,
            Set.of(),
            Instant.parse("2026-06-05T23:59:00Z"),
            Instant.parse("2026-06-02T18:00:00Z"),
            60);

    assertThat(occ).first().isEqualTo(Instant.parse("2026-06-03T18:00:00Z"));
  }
}
