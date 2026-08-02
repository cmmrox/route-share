package com.routeshare.reliability.service.impl;

import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.entity.MonthlyCounterEntity;
import com.routeshare.reliability.entity.ReliabilityEventEntity;
import com.routeshare.reliability.repository.MonthlyCounterRepository;
import com.routeshare.reliability.repository.ReliabilityEventRepository;
import com.routeshare.reliability.service.ReliabilityGateService;
import com.routeshare.reliability.service.ReliabilityService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReliabilityServiceImpl implements ReliabilityService {

  private final ReliabilityEventRepository events;
  private final MonthlyCounterRepository counters;
  private final ReliabilityGateService gates;
  private final Clock clock;

  @Autowired
  public ReliabilityServiceImpl(
      ReliabilityEventRepository events,
      MonthlyCounterRepository counters,
      ReliabilityGateService gates,
      Clock clock) {
    this.events = events;
    this.counters = counters;
    this.gates = gates;
    this.clock = clock;
  }

  @Override
  @Transactional
  public MonthlyCounterEntity record(
      long appUserId,
      ReliabilityRole role,
      ReliabilityEventType type,
      Long bookingId,
      Long tripId,
      String metadata) {
    Instant occurredAt = clock.instant();
    events.save(
        ReliabilityEventEntity.of(appUserId, role, type, occurredAt, bookingId, tripId, metadata));

    MonthlyCounterEntity counter = counter(appUserId, role, period(occurredAt));
    counter.apply(type);
    counter.setUpdatedAt(occurredAt);
    MonthlyCounterEntity saved = counters.save(counter);

    // The consequences of a counter reaching its limit belong with the counter, not with whichever
    // caller happened to record the event. A missed start recorded from anywhere must trigger the
    // same rule, or the rule is only as reliable as the last place somebody remembered it.
    gates.onCounterChanged(appUserId, role, type, saved);
    return saved;
  }

  @Override
  @Transactional
  public MonthlyCounterEntity counter(long appUserId, ReliabilityRole role, LocalDate periodMonth) {
    return counters
        .findByAppUserIdAndRoleAndPeriodMonth(appUserId, role, periodMonth)
        .orElseGet(() -> counters.save(MonthlyCounterEntity.opened(appUserId, role, periodMonth)));
  }

  @Override
  public LocalDate currentPeriod() {
    return period(clock.instant());
  }

  @Override
  @Transactional
  public MonthlyCounterEntity rebuild(long appUserId, ReliabilityRole role, LocalDate periodMonth) {
    Instant from = periodMonth.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant to = periodMonth.plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();

    MonthlyCounterEntity fresh = MonthlyCounterEntity.opened(appUserId, role, periodMonth);
    for (Object[] row : events.tallyByType(appUserId, role, from, to)) {
      ReliabilityEventType type = (ReliabilityEventType) row[0];
      long count = ((Number) row[1]).longValue();
      for (long i = 0; i < count; i++) {
        fresh.apply(type);
      }
    }

    MonthlyCounterEntity existing = counter(appUserId, role, periodMonth);
    copyCounts(fresh, existing);
    existing.setUpdatedAt(clock.instant());
    return counters.save(existing);
  }

  /**
   * The month boundary is UTC, matching every other timestamp in the system. A local-midnight
   * boundary would put a 23:30 no-show in a different month for the rider than for the report.
   */
  private LocalDate period(Instant at) {
    return LocalDate.ofInstant(at, ZoneOffset.UTC).withDayOfMonth(1);
  }

  private void copyCounts(MonthlyCounterEntity from, MonthlyCounterEntity to) {
    to.setMissedStarts(from.getMissedStarts());
    to.setLateCancellations(from.getLateCancellations());
    to.setStartExtensionsUsed(from.getStartExtensionsUsed());
    to.setNoShows(from.getNoShows());
    to.setLateCancels(from.getLateCancels());
    to.setEarlyDropsAdjusted(from.getEarlyDropsAdjusted());
    to.setTripsCompleted(from.getTripsCompleted());
    to.setTripsBooked(from.getTripsBooked());
    to.setOnTimeEvents(from.getOnTimeEvents());
    to.setOnTimeOpportunities(from.getOnTimeOpportunities());
  }
}
