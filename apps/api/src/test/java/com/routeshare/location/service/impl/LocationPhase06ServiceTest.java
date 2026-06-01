package com.routeshare.location.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.location.cache.InMemoryLatestLocationCache;
import com.routeshare.location.dto.request.DriverLocationUpdateRequest;
import com.routeshare.location.event.LocationRealtimePublisher;
import com.routeshare.location.repository.LocationSampleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LocationPhase06ServiceTest {
  private static final Instant NOW = Instant.parse("2026-06-02T00:00:00Z");

  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identity = mock(IdentityFacade.class);
  private final LocationSampleRepository repo = mock(LocationSampleRepository.class);
  private final LocationRealtimePublisher publisher = mock(LocationRealtimePublisher.class);
  private final InMemoryLatestLocationCache cache = new InMemoryLatestLocationCache();
  private final LocationServiceImpl service =
      new LocationServiceImpl(
          current, identity, repo, cache, publisher, Clock.fixed(NOW, ZoneOffset.UTC));

  LocationPhase06ServiceTest() {
    when(current.requireCurrentUser())
        .thenReturn(
            new CurrentUser("driver-sub", "d@example.com", null, "Driver", Set.of("DRIVER")));
    when(identity.upsertFromToken(any()))
        .thenReturn(
            new AppUser(
                44L, UUID.randomUUID(), "driver-sub", "d@example.com", null, "Driver", "ACTIVE"));
    when(repo.findDriverProfileIdForActiveTrip(10L, 44L)).thenReturn(java.util.Optional.of(7L));
  }

  @Test
  void acceptsFreshAccurateDriverUpdateAndPublishesLatestSnapshot() {
    var response =
        service.ingestDriverLocation(10L, update(6.9271, 79.8612, NOW.minusSeconds(5), 8.0, 12.0));

    assertThat(response.accepted()).isTrue();
    assertThat(response.tripId()).isEqualTo(10L);
    assertThat(cache.findByTripId(10L)).isPresent();
    verify(repo)
        .insertSample(
            eq(10L),
            eq(7L),
            eq(79.8612),
            eq(6.9271),
            eq(8.0),
            eq(12.0),
            eq(90.0),
            eq(NOW.minusSeconds(5)));
    verify(repo).insertLocationEvent(eq(10L), eq(7L), eq("DRIVER_LOCATION_ACCEPTED"), any());
    verify(publisher).publishTripLocation(eq(response));
  }

  @Test
  void rejectsLowAccuracyUpdatesBeforeCacheDatabaseOrWebsocketSideEffects() {
    assertThatThrownBy(
            () -> service.ingestDriverLocation(10L, update(6.9271, 79.8612, NOW, 250.0, 5.0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("accuracy");

    assertThat(cache.findByTripId(10L)).isEmpty();
    verify(repo, never())
        .insertSample(any(Long.class), any(Long.class), any(), any(), any(), any(), any(), any());
    verify(publisher, never()).publishTripLocation(any());
  }

  @Test
  void rejectsImpossibleJumpAgainstRedisLatestSnapshot() {
    service.ingestDriverLocation(10L, update(6.9271, 79.8612, NOW.minusSeconds(20), 8.0, 10.0));

    assertThatThrownBy(
            () ->
                service.ingestDriverLocation(
                    10L, update(7.8731, 80.7718, NOW.minusSeconds(15), 8.0, 10.0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("jump");
  }

  private DriverLocationUpdateRequest update(
      double lat, double lng, Instant at, double accuracy, double speed) {
    return new DriverLocationUpdateRequest(
        lat, lng, accuracy, speed, 90.0, at, "ios", 82.0, "wifi");
  }
}
