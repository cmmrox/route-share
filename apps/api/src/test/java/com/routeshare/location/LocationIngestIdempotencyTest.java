package com.routeshare.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.routeshare.common.ratelimit.*;
import com.routeshare.common.security.*;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.location.cache.*;
import com.routeshare.location.domain.*;
import com.routeshare.location.dto.request.*;
import com.routeshare.location.event.LocationRealtimePublisher;
import com.routeshare.location.repository.LocationPipelineRepository;
import com.routeshare.location.service.*;
import com.routeshare.location.service.impl.LocationPipelineServiceImpl;
import com.routeshare.trip.facade.TripArrivalFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class LocationIngestIdempotencyTest {
  @Test
  void replayedSampleReturnsTypedDuplicateWithoutASecondTrailRow() {
    var current = mock(CurrentUserProvider.class);
    var identity = mock(IdentityFacade.class);
    var repository = mock(LocationPipelineRepository.class);
    var access = mock(LocationPipelineRepository.DriverTripAccessRow.class);
    var policy = mock(LocationPipelineRepository.PolicyStateRow.class);
    when(current.requireCurrentUser())
        .thenReturn(new CurrentUser("driver", "d@example.com", null, "Driver", Set.of("DRIVER")));
    when(identity.upsertFromToken(any()))
        .thenReturn(new AppUser(4L, UUID.randomUUID(), "driver", null, null, "Driver", "ACTIVE"));
    when(access.getTripStatus()).thenReturn("STARTED");
    when(access.getDriverProfileId()).thenReturn(8L);
    when(repository.driverTripAccess(9L, 4L)).thenReturn(Optional.of(access));
    when(repository.claimSample(9L, "same")).thenReturn(0);
    when(repository.policyState(4L)).thenReturn(policy);
    var service =
        new LocationPipelineServiceImpl(
            current,
            identity,
            repository,
            new LocationFilterChain(50, 40, 80, 0.005),
            new DeadReckoner(20),
            new EtaCalculator(22),
            new RouteProjector(),
            new LocationPolicyResolver(2),
            new InMemoryLatestLocationCache(),
            mock(LocationRealtimePublisher.class),
            mock(TripArrivalFacade.class),
            mock(ApproachService.class),
            new SimpleMeterRegistry(),
            Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneOffset.UTC),
            mock(RateLimiter.class),
            new RateLimitProperties(true, null, null, null, null),
            60);
    var request =
        new LocationBatchUpdateRequest(
            List.of(
                new LocationSampleRequest(
                    "same",
                    Instant.parse("2026-08-04T00:00:00Z"),
                    6.9,
                    79.8,
                    10.0,
                    8.0,
                    90.0,
                    80)));

    var response = service.ingest(9L, request);

    assertThat(response.accepted()).isZero();
    assertThat(response.rejected())
        .singleElement()
        .satisfies(item -> assertThat(item.reason()).isEqualTo(LocationRejectionReason.DUPLICATE));
    verify(repository, never())
        .insertObservation(
            anyLong(),
            anyLong(),
            anyString(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            any(),
            any(),
            any(),
            any(),
            anyBoolean(),
            any(),
            any(),
            any());
  }
}
