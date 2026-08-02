package com.routeshare.scheduling.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.scheduling.domain.ScheduledJob;
import com.routeshare.scheduling.entity.JobRunEntity;
import com.routeshare.scheduling.repository.JobRunRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobRunnerImplTest {
  private static final Instant NOW = Instant.parse("2026-08-02T10:00:00Z");

  @Mock private JobRunRepository runs;

  private MeterRegistry meters;
  private JobRunnerImpl runner;

  @BeforeEach
  void setUp() {
    meters = new SimpleMeterRegistry();
    runner = new JobRunnerImpl(runs, meters, Clock.fixed(NOW, ZoneOffset.UTC));
    when(runs.save(any(JobRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void aSuccessfulRunIsRecordedWithItsProcessedCount() {
    int processed = runner.run(job("start-buffer-expiry", () -> 7));

    assertThat(processed).isEqualTo(7);
    JobRunEntity saved = lastSaved();
    assertThat(saved.getJobName()).isEqualTo("start-buffer-expiry");
    assertThat(saved.getStatus()).isEqualTo(JobRunEntity.STATUS_SUCCEEDED);
    assertThat(saved.getProcessedCount()).isEqualTo(7);
    assertThat(saved.getFinishedAt()).isEqualTo(NOW);
    assertThat(counter("routeshare_job_processed_total")).isEqualTo(7.0);
  }

  /**
   * A tick sweeps several independent clocks. If one throws and the exception escapes, the clocks
   * after it in the list never run — so a wedged pickup-wait sweep would silently stop trips from
   * auto-cancelling.
   */
  @Test
  void aFailingJobIsRecordedAndDoesNotPropagate() {
    assertThatCode(
            () ->
                runner.run(
                    job(
                        "pickup-wait-expiry",
                        () -> {
                          throw new IllegalStateException("database went away");
                        })))
        .doesNotThrowAnyException();

    JobRunEntity saved = lastSaved();
    assertThat(saved.getStatus()).isEqualTo(JobRunEntity.STATUS_FAILED);
    assertThat(saved.getError()).contains("database went away");
    assertThat(saved.getFinishedAt()).isEqualTo(NOW);
  }

  @Test
  void aFailedRunReportsZeroProcessedRatherThanGuessing() {
    assertThat(
            runner.run(
                job(
                    "driver-late-grace",
                    () -> {
                      throw new IllegalStateException("boom");
                    })))
        .isZero();
    assertThat(counter("routeshare_job_processed_total")).isZero();
  }

  /** A run that found nothing is still a run: it is the evidence the sweeper is alive. */
  @Test
  void aRunThatFoundNothingIsStillRecorded() {
    runner.run(job("monthly-counter-reset", () -> 0));

    JobRunEntity saved = lastSaved();
    assertThat(saved.getStatus()).isEqualTo(JobRunEntity.STATUS_SUCCEEDED);
    assertThat(saved.getProcessedCount()).isZero();
  }

  @Test
  void runsAreTaggedByJobAndStatusSoOneDeadSweeperIsDistinguishable() {
    runner.run(job("start-buffer-expiry", () -> 1));
    runner.run(
        job(
            "pickup-wait-expiry",
            () -> {
              throw new IllegalStateException("boom");
            }));

    assertThat(
            meters
                .counter(
                    "routeshare_job_runs_total",
                    "job",
                    "start-buffer-expiry",
                    "status",
                    "SUCCEEDED")
                .count())
        .isEqualTo(1.0);
    assertThat(
            meters
                .counter(
                    "routeshare_job_runs_total", "job", "pickup-wait-expiry", "status", "FAILED")
                .count())
        .isEqualTo(1.0);
  }

  private double counter(String name) {
    return meters.find(name).counters().stream().mapToDouble(c -> c.count()).sum();
  }

  private JobRunEntity lastSaved() {
    ArgumentCaptor<JobRunEntity> captor = ArgumentCaptor.forClass(JobRunEntity.class);
    verify(runs, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
    List<JobRunEntity> all = new ArrayList<>(captor.getAllValues());
    return all.get(all.size() - 1);
  }

  private ScheduledJob job(String name, Body body) {
    return new ScheduledJob() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public int run() {
        return body.run();
      }
    };
  }

  @FunctionalInterface
  private interface Body {
    int run();
  }
}
