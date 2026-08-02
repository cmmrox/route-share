package com.routeshare.booking.job;

import com.routeshare.booking.service.BookingExpiryService;
import com.routeshare.scheduling.domain.ScheduledJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Closes booking requests the driver never answered, releasing their seats (D16). */
@Component
public class ScheduledRequestExpiryJob implements ScheduledJob {

  private final BookingExpiryService expiries;
  private final int batchSize;

  @Autowired
  public ScheduledRequestExpiryJob(
      BookingExpiryService expiries,
      @Value("${routeshare.scheduler.batch-size:200}") int batchSize) {
    this.expiries = expiries;
    this.batchSize = batchSize;
  }

  @Override
  public String name() {
    return "scheduled-request-expiry";
  }

  @Override
  public int run() {
    return expiries.sweepExpired(batchSize);
  }
}
