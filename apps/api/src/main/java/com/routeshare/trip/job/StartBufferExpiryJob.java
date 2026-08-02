package com.routeshare.trip.job;

import com.routeshare.scheduling.domain.ScheduledJob;
import com.routeshare.trip.service.TripStartWindowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Finds trips past their start buffer that never started, and cancels them.
 *
 * <p>Registered simply by being a {@link ScheduledJob} bean; {@code JobRegistry} sweeps it under
 * the leader lock.
 */
@Component
public class StartBufferExpiryJob implements ScheduledJob {

  private final TripStartWindowService windows;
  private final int batchSize;

  @Autowired
  public StartBufferExpiryJob(
      TripStartWindowService windows,
      @Value("${routeshare.scheduler.batch-size:200}") int batchSize) {
    this.windows = windows;
    this.batchSize = batchSize;
  }

  @Override
  public String name() {
    return "start-buffer-expiry";
  }

  @Override
  public int run() {
    return windows.sweepExpired(batchSize);
  }
}
