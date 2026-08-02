package com.routeshare.scheduling.domain;

/**
 * A sweep that finds expired rows and applies the transition each one is due.
 *
 * <p>Implementations must be idempotent at the row level. Leader election makes concurrent runs
 * unlikely, not impossible — a lock can expire under a long GC pause — so correctness rests on the
 * row transition refusing to apply twice, not on the lock.
 *
 * <p>A job returns how many rows it processed so {@code JobRunner} can record it; throwing is
 * allowed and is recorded as a failed run.
 */
public interface ScheduledJob {

  /** Stable identifier, used as the ShedLock key, the metric tag and the {@code job_run} name. */
  String name();

  /**
   * @return the number of rows this sweep resolved.
   */
  int run();
}
