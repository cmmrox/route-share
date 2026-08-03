package com.routeshare.location.service.impl;

import com.routeshare.location.repository.LocationPipelineRepository;
import com.routeshare.location.service.*;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationMaintenanceServiceImpl implements LocationMaintenanceService {
  private final LocationPipelineRepository progress;
  private final ApproachService approaches;
  private final RealtimeChannelService realtime;
  private final Clock clock;
  private final long extrapolationMaximumSeconds;
  private final long retentionDays;

  public LocationMaintenanceServiceImpl(
      LocationPipelineRepository progress,
      ApproachService approaches,
      RealtimeChannelService realtime,
      Clock clock,
      @Value("${routeshare.location.extrapolation-max-seconds:20}")
          long extrapolationMaximumSeconds,
      @Value("${routeshare.location.sample-retention-days:90}") long retentionDays) {
    this.progress = progress;
    this.approaches = approaches;
    this.realtime = realtime;
    this.clock = clock;
    this.extrapolationMaximumSeconds = extrapolationMaximumSeconds;
    this.retentionDays = retentionDays;
  }

  @Override
  @Transactional
  public int sweepStaleness(int batchSize) {
    Instant now = Instant.now(clock);
    int changed = progress.sweepConfidence(now, now.minusSeconds(extrapolationMaximumSeconds));
    changed += approaches.closeStaleSessions(batchSize);
    changed += realtime.purgeExpired();
    return changed;
  }

  @Override
  @Transactional
  public int retainSamples() {
    progress.ensurePartitions();
    return progress.deleteSamplesBefore(Instant.now(clock).minus(Duration.ofDays(retentionDays)));
  }
}
