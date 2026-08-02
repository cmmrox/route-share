package com.routeshare.reliability.job;

import com.routeshare.reliability.service.ReliabilityPanelService;
import com.routeshare.scheduling.domain.ScheduledJob;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Closes the month and opens the next.
 *
 * <p>Runs on every tick but does its work only on the first of the month, rather than being wired
 * to a cron expression: the sweeper already holds the leader lock, and a second scheduling
 * mechanism would be a second thing to get wrong about time zones.
 */
@Component
@RequiredArgsConstructor
public class MonthlyCounterResetJob implements ScheduledJob {

  private final ReliabilityPanelService panels;
  private final Clock clock;

  @Override
  public String name() {
    return "monthly-counter-reset";
  }

  @Override
  public int run() {
    LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    if (today.getDayOfMonth() != 1) {
      return 0;
    }
    return panels.rolloverMonth();
  }
}
