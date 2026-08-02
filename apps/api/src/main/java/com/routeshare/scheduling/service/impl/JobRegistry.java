package com.routeshare.scheduling.service.impl;

import com.routeshare.scheduling.domain.ScheduledJob;
import com.routeshare.scheduling.service.JobRunner;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The single tick. Every {@link ScheduledJob} bean in the context is discovered and swept here, so
 * a new clock is registered by existing as a bean rather than by wiring another {@code @Scheduled}
 * method — slices 07, 10, 11, 12, 13 and 14 each add jobs against this.
 *
 * <p>One lock covers the whole tick rather than one per job. The jobs are short and share a
 * database; per-job locks would multiply lock churn without letting anything overlap usefully.
 */
@Component
@ConditionalOnProperty(
    name = "routeshare.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class JobRegistry {
  private static final Logger log = LoggerFactory.getLogger(JobRegistry.class);

  private final List<ScheduledJob> jobs;
  private final JobRunner runner;

  @Autowired
  public JobRegistry(List<ScheduledJob> jobs, JobRunner runner) {
    this.jobs = jobs;
    this.runner = runner;
    log.info(
        "scheduler registered {} jobs: {}",
        jobs.size(),
        jobs.stream().map(ScheduledJob::name).toList());
  }

  /**
   * {@code lockAtLeastFor} guards against a tick so fast that a second instance whose clock is a
   * little ahead acquires the lock again within the same wall-clock second.
   */
  @Scheduled(fixedDelayString = "${routeshare.scheduler.tick-seconds:60}000")
  @SchedulerLock(
      name = "routeshare-scheduler-tick",
      lockAtLeastFor = "PT10S",
      lockAtMostFor = "PT5M")
  public void tick() {
    for (ScheduledJob job : jobs) {
      runner.run(job);
    }
  }

  public List<ScheduledJob> registered() {
    return List.copyOf(jobs);
  }
}
