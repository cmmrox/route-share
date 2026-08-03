package com.routeshare.location.job;

import com.routeshare.location.service.LocationMaintenanceService;
import com.routeshare.scheduling.domain.ScheduledJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationStalenessJob implements ScheduledJob {
  private final LocationMaintenanceService maintenance;

  @Override
  public String name() {
    return "location-staleness-sweep";
  }

  @Override
  public int run() {
    return maintenance.sweepStaleness(500);
  }
}
