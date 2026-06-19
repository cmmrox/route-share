package com.routeshare.routing.domain;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RouteSchedulePolicy {
  /** Hard cap on occurrences generated in one call, to bound recurring expansion. */
  public static final int MAX_RECURRING_OCCURRENCES = 60;

  private final Clock clock;

  public RouteSchedulePolicy(Clock clock) {
    this.clock = clock;
  }

  public List<Instant> generateOneTimeOccurrences(Instant departureTime) {
    if (departureTime == null || !departureTime.isAfter(Instant.now(clock))) {
      throw new IllegalArgumentException("Route departure time must be in the future");
    }
    return List.of(departureTime);
  }

  /**
   * Expands a recurring schedule into concrete future departure instants. Starting from {@code
   * firstDeparture}'s date at its UTC time-of-day, it walks day by day and keeps each day whose
   * weekday is in {@code days} (an empty set means every day), stopping at {@code notBefore.plus
   * horizon}/{@code endAt}, the cap, or after {@code maxToGenerate}. {@code alreadyScheduledAfter}
   * lets callers extend an existing series without duplicating past occurrences.
   */
  public List<Instant> generateRecurringOccurrences(
      Instant firstDeparture,
      Instant endAt,
      Set<DayOfWeek> days,
      Instant generateUntil,
      Instant skipUpToAndIncluding,
      int maxToGenerate) {
    if (firstDeparture == null) {
      throw new IllegalArgumentException("Recurring routes require a first departure time");
    }
    Instant now = Instant.now(clock);
    Instant horizon = generateUntil;
    if (endAt != null && endAt.isBefore(horizon)) {
      horizon = endAt;
    }
    int cap =
        Math.min(
            maxToGenerate <= 0 ? MAX_RECURRING_OCCURRENCES : maxToGenerate,
            MAX_RECURRING_OCCURRENCES);

    ZonedDateTime cursor = firstDeparture.atZone(ZoneOffset.UTC);
    List<Instant> result = new ArrayList<>();
    while (result.size() < cap) {
      Instant candidate = cursor.toInstant();
      if (candidate.isAfter(horizon)) {
        break;
      }
      boolean dayMatches = days == null || days.isEmpty() || days.contains(cursor.getDayOfWeek());
      boolean future = candidate.isAfter(now);
      boolean afterExisting =
          skipUpToAndIncluding == null || candidate.isAfter(skipUpToAndIncluding);
      if (dayMatches && future && afterExisting) {
        result.add(candidate);
      }
      cursor = cursor.plusDays(1);
    }
    return result;
  }
}
