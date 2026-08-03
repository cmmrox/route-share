package com.routeshare.location.event;

import com.routeshare.location.dto.response.LocationUpdateResponse;
import com.routeshare.location.repository.LocationSampleRepository;
import com.routeshare.location.service.RealtimeStreamService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketLocationRealtimePublisher implements LocationRealtimePublisher {
  private final LocationSampleRepository locations;
  private final RealtimeStreamService realtime;

  @Override
  public void publishTripLocation(LocationUpdateResponse response) {
    if (response == null || response.tripId() == null || response.latestLocation() == null) {
      return;
    }
    var location = response.latestLocation();
    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("tripId", String.valueOf(response.tripId()));
    payload.put("latitude", String.valueOf(location.latitude()));
    payload.put("longitude", String.valueOf(location.longitude()));
    payload.put("accuracyMeters", String.valueOf(location.accuracyMeters()));
    payload.put("serverReceivedAt", String.valueOf(response.serverReceivedAt()));
    payload.put("stale", String.valueOf(location.stale()));
    for (Long appUserId : locations.authorizedRealtimeAppUserIds(response.tripId())) {
      realtime.deliver(appUserId, "TRIP_LOCATION_UPDATED", payload);
    }
  }
}
