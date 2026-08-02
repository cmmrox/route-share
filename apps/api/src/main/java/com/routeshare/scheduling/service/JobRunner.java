package com.routeshare.scheduling.service;

import com.routeshare.scheduling.domain.ScheduledJob;

/** Runs a job with recording, metrics and failure isolation around it. */
public interface JobRunner {

  /**
   * Executes {@code job}, writing a {@code scheduling.job_run} row and emitting metrics whatever
   * the outcome. Never propagates: one wedged sweeper must not take the scheduler thread, and
   * therefore every other clock, down with it.
   *
   * @return rows processed, or 0 if the job failed.
   */
  int run(ScheduledJob job);
}
