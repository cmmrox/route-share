package com.routeshare.trip.job;

import com.routeshare.scheduling.domain.ScheduledJob;
import com.routeshare.trip.service.DriverLateGraceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Unlocks the free cancel for passengers whose driver has not turned up. */
@Component
public class DriverLateGraceJob implements ScheduledJob {

  private final DriverLateGraceService graces;
  private final int batchSize;

  @Autowired
  public DriverLateGraceJob(
      DriverLateGraceService graces,
      @Value("${routeshare.scheduler.batch-size:200}") int batchSize) {
    this.graces = graces;
    this.batchSize = batchSize;
  }

  @Override
  public String name() {
    return "driver-late-grace";
  }

  @Override
  public int run() {
    return graces.sweepExpired(batchSize);
  }
}
