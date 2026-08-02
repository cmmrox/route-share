package com.routeshare.reliability.service;

import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.domain.ReliabilityRole;
import com.routeshare.reliability.entity.MonthlyCounterEntity;
import java.time.LocalDate;

/** The append-only reliability log and the monthly counters projected from it. */
public interface ReliabilityService {

  /**
   * Records one event and applies it to the current month's counter, in the caller's transaction.
   *
   * <p>Same transaction on purpose: a no-show that releases a seat but fails to record why is a
   * penalty with no trail, and support cannot defend it.
   */
  MonthlyCounterEntity record(
      long appUserId,
      ReliabilityRole role,
      ReliabilityEventType type,
      Long bookingId,
      Long tripId,
      String metadata);

  /** The counter for a given month, opened empty if this user has no events in it yet. */
  MonthlyCounterEntity counter(long appUserId, ReliabilityRole role, LocalDate periodMonth);

  /** The calendar month, in the platform's timezone, that {@code now} falls in. */
  LocalDate currentPeriod();

  /**
   * Recomputes a month's counter from the event log and overwrites the projection. The projection
   * is a cache; this is what makes it safe to treat it as one.
   */
  MonthlyCounterEntity rebuild(long appUserId, ReliabilityRole role, LocalDate periodMonth);
}
