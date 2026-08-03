package com.routeshare.location;

import static org.mockito.Mockito.*;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.location.domain.EtaCalculator;
import com.routeshare.location.repository.ApproachSessionRepository;
import com.routeshare.location.service.impl.ApproachServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApproachSessionIT {
  @Test
  void opensAtFiveHundredMetresAndCloseDeletesRiderPosition() {
    var sessions = mock(ApproachSessionRepository.class);
    var pickup = mock(ApproachSessionRepository.NextPickupRow.class);
    when(pickup.getBookingId()).thenReturn(12L);
    when(pickup.getDistanceMeters()).thenReturn(499.9);
    when(sessions.nextPickup(9L)).thenReturn(java.util.Optional.of(pickup));
    when(sessions.open(9L, 12L)).thenReturn(1);
    when(sessions.staleSessionIds(any(), eq(500))).thenReturn(List.of(3L));
    when(sessions.closeAndDeleteRiderPosition(eq(3L), any())).thenReturn(1);
    var service =
        new ApproachServiceImpl(
            mock(CurrentUserProvider.class),
            mock(IdentityFacade.class),
            sessions,
            new EtaCalculator(22),
            new SimpleMeterRegistry(),
            Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneOffset.UTC));
    org.springframework.test.util.ReflectionTestUtils.setField(
        service, "approachRadiusMeters", 500d);

    service.evaluateForTrip(9L);
    service.closeStaleSessions(500);

    verify(sessions).open(9L, 12L);
    verify(sessions).closeAndDeleteRiderPosition(eq(3L), any());
  }
}
