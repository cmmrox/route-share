package com.routeshare.location.event;

import com.routeshare.location.dto.response.LocationUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketLocationRealtimePublisher implements LocationRealtimePublisher {
  private final SimpMessagingTemplate messagingTemplate;

  @Override
  public void publishTripLocation(LocationUpdateResponse response) {
    messagingTemplate.convertAndSend(
        "/topic/trips/" + response.tripId() + "/location", response.latestLocation());
    messagingTemplate.convertAndSend("/topic/admin/trips/live", response);
  }
}
