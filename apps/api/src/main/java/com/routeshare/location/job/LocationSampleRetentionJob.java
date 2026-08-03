package com.routeshare.location.job;

import com.routeshare.location.service.LocationMaintenanceService;
import com.routeshare.scheduling.domain.ScheduledJob;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationSampleRetentionJob implements ScheduledJob {
  private final LocationMaintenanceService maintenance;
  private final AtomicReference<LocalDate> lastRun = new AtomicReference<>();

  @Override
  public String name() {
    return "location-sample-retention";
  }

  @Override
  public int run() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    if (today.equals(lastRun.get())) {
      return 0;
    }
    int removed = maintenance.retainSamples();
    lastRun.set(today);
    return removed;
  }
}
