package com.routeshare.reliability.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.entity.MonthlyCounterEntity;
import com.routeshare.reliability.entity.ReliabilityEventEntity;
import com.routeshare.reliability.repository.MonthlyCounterRepository;
import com.routeshare.reliability.repository.ReliabilityEventRepository;
import com.routeshare.reliability.service.ReliabilityGateService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Counters as projections of the event log, and what a month boundary does to them.
 *
 * <p>The point of projecting rather than incrementing in place is that a correction is possible and
 * D28/P39 can show what happened rather than only a number. These cases assert both halves: the
 * live path and a rebuild must agree, and crossing into a new month must leave the old one readable
 * — a rider asking in September why she is prepaying is asking about August.
 */
class ReliabilityCounterTest {
  private static final LocalDate AUGUST = LocalDate.of(2026, 8, 1);
  private static final LocalDate SEPTEMBER = LocalDate.of(2026, 9, 1);
  private static final long PASSENGER = 12L;

  private final ReliabilityEventRepository events = mock(ReliabilityEventRepository.class);
  private final MonthlyCounterRepository counters = mock(MonthlyCounterRepository.class);
  private final ReliabilityGateService gates = mock(ReliabilityGateService.class);

  /** An in-memory stand-in for the counter table, keyed the way the unique constraint keys it. */
  private final List<MonthlyCounterEntity> stored = new ArrayList<>();

  private ReliabilityServiceImpl serviceAt(Instant now) {
    when(counters.save(any(MonthlyCounterEntity.class)))
        .thenAnswer(
            invocation -> {
              MonthlyCounterEntity c = invocation.getArgument(0);
              stored.removeIf(
                  e ->
                      e.getAppUserId().equals(c.getAppUserId())
                          && e.getRole() == c.getRole()
                          && e.getPeriodMonth().equals(c.getPeriodMonth()));
              stored.add(c);
              return c;
            });
    when(counters.findByAppUserIdAndRoleAndPeriodMonth(anyLong(), any(), any()))
        .thenAnswer(
            invocation ->
                stored.stream()
                    .filter(
                        e ->
                            e.getAppUserId().equals(invocation.getArgument(0))
                                && e.getRole() == invocation.getArgument(1)
                                && e.getPeriodMonth().equals(invocation.getArgument(2)))
                    .findFirst());
    return new ReliabilityServiceImpl(events, counters, gates, Clock.fixed(now, ZoneOffset.UTC));
  }

  private Instant inAugust() {
    return Instant.parse("2026-08-20T10:00:00Z");
  }

  private Instant inSeptember() {
    return Instant.parse("2026-09-02T10:00:00Z");
  }

  @Test
  void recordingAnEventAppendsToTheLogAndMovesTheMonthsCounter() {
    var service = serviceAt(inAugust());

    var counter =
        service.record(
            PASSENGER, ReliabilityRole.PASSENGER, ReliabilityEventType.NO_SHOW, 100L, 77L, null);

    assertThat(counter.getNoShows()).isEqualTo(1);
    assertThat(counter.getPeriodMonth()).isEqualTo(AUGUST);
    org.mockito.Mockito.verify(events).save(any(ReliabilityEventEntity.class));
  }

  /** 05-20: the same rider crossing a month boundary starts the new month clean. */
  @Test
  void crossingAMonthBoundaryStartsANewCounterAndLeavesTheOldOneIntact() {
    serviceAt(inAugust())
        .record(
            PASSENGER, ReliabilityRole.PASSENGER, ReliabilityEventType.NO_SHOW, 100L, 77L, null);
    serviceAt(inAugust())
        .record(
            PASSENGER, ReliabilityRole.PASSENGER, ReliabilityEventType.NO_SHOW, 101L, 78L, null);

    var september =
        serviceAt(inSeptember())
            .record(
                PASSENGER,
                ReliabilityRole.PASSENGER,
                ReliabilityEventType.NO_SHOW,
                102L,
                79L,
                null);

    assertThat(september.getPeriodMonth()).isEqualTo(SEPTEMBER);
    assertThat(september.getNoShows()).isEqualTo(1);

    // August is untouched, which is what makes "why am I prepaying?" answerable next month.
    var august =
        stored.stream().filter(c -> c.getPeriodMonth().equals(AUGUST)).findFirst().orElseThrow();
    assertThat(august.getNoShows()).isEqualTo(2);
  }

  /**
   * The projection is a cache, and this is what makes it safe to treat it as one: a rebuild from
   * the log overwrites whatever the counter said.
   */
  @Test
  void rebuildingAMonthRecomputesItFromTheEventLog() {
    var service = serviceAt(inAugust());
    var drifted = MonthlyCounterEntity.opened(PASSENGER, ReliabilityRole.PASSENGER, AUGUST);
    drifted.setNoShows(99);
    stored.add(drifted);

    when(events.tallyByType(anyLong(), any(), any(), any()))
        .thenReturn(List.<Object[]>of(new Object[] {ReliabilityEventType.NO_SHOW, 2L}));

    var rebuilt = service.rebuild(PASSENGER, ReliabilityRole.PASSENGER, AUGUST);

    assertThat(rebuilt.getNoShows()).isEqualTo(2);
  }

  /**
   * The month boundary is UTC, matching every other instant in the system. A local-midnight
   * boundary would file a 23:30 no-show in a different month for the rider than for the report.
   */
  @Test
  void theMonthBoundaryIsUtc() {
    var lateOnTheLastOfAugust = Instant.parse("2026-08-31T23:30:00Z");
    assertThat(serviceAt(lateOnTheLastOfAugust).currentPeriod()).isEqualTo(AUGUST);

    var justAfterMidnight = Instant.parse("2026-09-01T00:30:00Z");
    assertThat(serviceAt(justAfterMidnight).currentPeriod()).isEqualTo(SEPTEMBER);
  }

  @Test
  void aMonthWithNoEventsOpensEmptyRatherThanFailing() {
    var service = serviceAt(inSeptember());

    var counter = service.counter(PASSENGER, ReliabilityRole.PASSENGER, SEPTEMBER);

    assertThat(counter.getNoShows()).isZero();
    assertThat(Optional.of(counter.getPeriodMonth())).contains(SEPTEMBER);
  }
}
