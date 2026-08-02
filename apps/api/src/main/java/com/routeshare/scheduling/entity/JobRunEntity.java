package com.routeshare.scheduling.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One execution of one job. Written even when the job found nothing — see {@code JobRunner}. */
@Entity
@Table(name = "job_run", schema = "scheduling")
@Getter
@Setter
@NoArgsConstructor
public class JobRunEntity {
  public static final String STATUS_RUNNING = "RUNNING";
  public static final String STATUS_SUCCEEDED = "SUCCEEDED";
  public static final String STATUS_FAILED = "FAILED";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "job_run_id")
  private Long id;

  @Column(name = "job_name", nullable = false)
  private String jobName;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(nullable = false)
  private String status = STATUS_RUNNING;

  @Column(name = "processed_count", nullable = false)
  private int processedCount;

  @Column private String error;

  public static JobRunEntity started(String jobName, Instant at) {
    var entity = new JobRunEntity();
    entity.jobName = jobName;
    entity.startedAt = at;
    entity.status = STATUS_RUNNING;
    return entity;
  }

  public void succeeded(int processed, Instant at) {
    this.processedCount = processed;
    this.status = STATUS_SUCCEEDED;
    this.finishedAt = at;
  }

  public void failed(String message, Instant at) {
    this.status = STATUS_FAILED;
    // The column is unbounded but a stack trace in a status table helps nobody; the log has it.
    this.error =
        message == null ? "unknown" : message.substring(0, Math.min(message.length(), 500));
    this.finishedAt = at;
  }
}
