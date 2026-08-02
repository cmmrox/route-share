package com.routeshare.scheduling.service.impl;

import com.routeshare.scheduling.domain.ScheduledJob;
import com.routeshare.scheduling.repository.JobRunRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Reports DOWN when any job has not completed successfully within three ticks.
 *
 * <p>A dead sweeper is silent by nature: trips simply never auto-cancel and seats are never
 * released, which looks like an unusually quiet day rather than an outage. This is the only thing
 * that notices.
 */
@Component("schedulerHealthIndicator")
@ConditionalOnProperty(
    name = "routeshare.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SchedulerHealthIndicator implements HealthIndicator {
  private static final int TICKS_BEFORE_ALARM = 3;

  private final JobRegistry registry;
  private final JobRunRepository runs;
  private final Clock clock;
  private final long tickSeconds;

  @Autowired
  public SchedulerHealthIndicator(
      JobRegistry registry,
      JobRunRepository runs,
      Clock clock,
      @Value("${routeshare.scheduler.tick-seconds:60}") long tickSeconds) {
    this.registry = registry;
    this.runs = runs;
    this.clock = clock;
    this.tickSeconds = tickSeconds;
  }

  @Override
  public Health health() {
    var window = Duration.ofSeconds(tickSeconds * TICKS_BEFORE_ALARM);
    var since = clock.instant().minus(window);
    List<String> stale = new ArrayList<>();
    for (ScheduledJob job : registry.registered()) {
      if (runs.countSucceededSince(job.name(), since) == 0) {
        stale.add(job.name());
      }
    }
    var builder = stale.isEmpty() ? Health.up() : Health.down();
    return builder
        .withDetail("windowSeconds", window.toSeconds())
        .withDetail("staleJobs", stale)
        .build();
  }
}
