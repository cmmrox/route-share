package com.routeshare.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.routeshare.location.repository.LocationPipelineRepository;
import com.routeshare.location.service.*;
import com.routeshare.location.service.impl.LocationMaintenanceServiceImpl;
import java.time.*;
import org.junit.jupiter.api.Test;

class StalenessSweepIT {
  @Test
  void sweepDemotesProgressAndClosesPrivacyWindows() {
    var progress = mock(LocationPipelineRepository.class);
    var approaches = mock(ApproachService.class);
    var realtime = mock(RealtimeChannelService.class);
    Instant now = Instant.parse("2026-08-04T00:00:00Z");
    when(progress.sweepConfidence(now, now.minusSeconds(20))).thenReturn(2);
    when(approaches.closeStaleSessions(500)).thenReturn(1);
    when(realtime.purgeExpired()).thenReturn(1);
    var service =
        new LocationMaintenanceServiceImpl(
            progress, approaches, realtime, Clock.fixed(now, ZoneOffset.UTC), 20, 90);

    assertThat(service.sweepStaleness(500)).isEqualTo(4);
    verify(progress).sweepConfidence(now, now.minusSeconds(20));
    verify(approaches).closeStaleSessions(500);
  }
}
