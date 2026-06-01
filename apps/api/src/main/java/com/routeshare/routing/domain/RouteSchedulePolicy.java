package com.routeshare.routing.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RouteSchedulePolicy {
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
}
