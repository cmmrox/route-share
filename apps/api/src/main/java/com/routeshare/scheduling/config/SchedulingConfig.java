package com.routeshare.scheduling.config;

import java.time.Duration;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Leader election for every time-driven behaviour in the product.
 *
 * <p>Without it a two-instance deploy auto-cancels each trip twice and charges each no-show fee
 * twice — the jobs registered here move money and mark people's records, so "ran twice" is not a
 * performance problem, it is a refund and an apology.
 *
 * <p>The lock is held in {@code scheduling.shedlock} rather than in Redis: the transitions the jobs
 * perform are committed to Postgres, and a lock in a different store than the work can be lost
 * independently of it.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
@ConditionalOnProperty(
    name = "routeshare.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SchedulingConfig {

  /**
   * {@code lockAtMostFor} is the backstop for an instance that dies mid-sweep: the lock is released
   * after this even if nobody unlocks it. It must exceed the longest plausible run of any job, or a
   * second instance starts the same sweep while the first is still writing.
   */
  @Bean
  public LockProvider lockProvider(DataSource dataSource) {
    return new JdbcLockProvider(dataSource, "scheduling.shedlock");
  }

  @Bean
  public SchedulerProperties schedulerProperties() {
    return new SchedulerProperties(Duration.ofSeconds(60));
  }

  /** Tick cadence, kept out of the jobs so tests can drive them directly. */
  public record SchedulerProperties(Duration tick) {}
}
