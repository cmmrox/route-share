package com.routeshare.scheduling.service.impl;

import com.routeshare.scheduling.domain.ScheduledJob;
import com.routeshare.scheduling.entity.JobRunEntity;
import com.routeshare.scheduling.repository.JobRunRepository;
import com.routeshare.scheduling.service.JobRunner;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobRunnerImpl implements JobRunner {
  private static final Logger log = LoggerFactory.getLogger(JobRunnerImpl.class);

  private final JobRunRepository runs;
  private final MeterRegistry meters;
  private final Clock clock;

  @Autowired
  public JobRunnerImpl(JobRunRepository runs, MeterRegistry meters, Clock clock) {
    this.runs = runs;
    this.meters = meters;
    this.clock = clock;
  }

  /**
   * The {@code job_run} row is written in its own transaction, so a job whose work rolls back still
   * leaves a record that it ran and failed. A failure that erases its own evidence is the failure
   * mode this table exists to prevent.
   */
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int run(ScheduledJob job) {
    Instant startedAt = clock.instant();
    JobRunEntity run = runs.save(JobRunEntity.started(job.name(), startedAt));
    long start = System.nanoTime();
    try {
      int processed = job.run();
      run.succeeded(processed, clock.instant());
      runs.save(run);
      record(job, "SUCCEEDED", processed, start);
      if (processed > 0) {
        log.info("job {} processed {} rows", job.name(), processed);
      }
      return processed;
    } catch (RuntimeException ex) {
      run.failed(ex.getMessage(), clock.instant());
      runs.save(run);
      record(job, "FAILED", 0, start);
      // Swallowed deliberately: @Scheduled propagation cancels nothing, but a throw here would
      // abandon the remaining jobs in this tick, and they are independent clocks.
      log.error("job {} failed", job.name(), ex);
      return 0;
    }
  }

  private void record(ScheduledJob job, String status, int processed, long startNanos) {
    meters.counter("routeshare_job_runs_total", "job", job.name(), "status", status).increment();
    meters.counter("routeshare_job_processed_total", "job", job.name()).increment(processed);
    Timer.builder("routeshare_job_duration_seconds")
        .tag("job", job.name())
        .register(meters)
        .record(Duration.ofNanos(System.nanoTime() - startNanos));
  }
}
