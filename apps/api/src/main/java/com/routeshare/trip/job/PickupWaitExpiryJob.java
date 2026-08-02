package com.routeshare.trip.job;

import com.routeshare.scheduling.domain.ScheduledJob;
import com.routeshare.trip.service.PickupWaitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Finds pickup waits that have run out and releases the seat as a no-show. */
@Component
public class PickupWaitExpiryJob implements ScheduledJob {

  private final PickupWaitService waits;
  private final int batchSize;

  @Autowired
  public PickupWaitExpiryJob(
      PickupWaitService waits, @Value("${routeshare.scheduler.batch-size:200}") int batchSize) {
    this.waits = waits;
    this.batchSize = batchSize;
  }

  @Override
  public String name() {
    return "pickup-wait-expiry";
  }

  @Override
  public int run() {
    return waits.sweepExpired(batchSize);
  }
}
