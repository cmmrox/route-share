package com.routeshare.location.event;

import static org.mockito.Mockito.*;

import com.routeshare.location.dto.response.LocationSnapshotResponse;
import com.routeshare.location.dto.response.LocationUpdateResponse;
import com.routeshare.location.repository.LocationSampleRepository;
import com.routeshare.location.service.RealtimeStreamService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class WebSocketLocationRealtimePublisherTest {
  @Test
  void sendsOnlyToServerResolvedTripParticipants() {
    LocationSampleRepository locations = mock(LocationSampleRepository.class);
    RealtimeStreamService realtime = mock(RealtimeStreamService.class);
    when(locations.authorizedRealtimeAppUserIds(42L)).thenReturn(List.of(7L, 9L));
    var publisher = new WebSocketLocationRealtimePublisher(locations, realtime);
    Instant now = Instant.parse("2026-08-04T00:00:00Z");
    var snapshot =
        new LocationSnapshotResponse(42L, 3L, 6.9271, 79.8612, 8.0, 5.0, 90.0, now, now, false);

    publisher.publishTripLocation(new LocationUpdateResponse(true, 42L, 3L, now, snapshot));

    verify(realtime).deliver(eq(7L), eq("TRIP_LOCATION_UPDATED"), anyMap());
    verify(realtime).deliver(eq(9L), eq("TRIP_LOCATION_UPDATED"), anyMap());
    verifyNoMoreInteractions(realtime);
  }
}
