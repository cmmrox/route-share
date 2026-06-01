package com.routeshare.location.event;

import com.routeshare.location.dto.response.LocationUpdateResponse;

public interface LocationRealtimePublisher {
  void publishTripLocation(LocationUpdateResponse response);
}
