package com.routeshare.location.domain;

import com.routeshare.location.dto.response.LocationPolicyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocationPolicyResolver {
  private final int approachIntervalSeconds;

  public LocationPolicyResolver(
      @Value("${routeshare.location.approach-sample-interval-seconds:2}")
          int approachIntervalSeconds) {
    this.approachIntervalSeconds = approachIntervalSeconds;
  }

  public LocationPolicyResponse resolve(
      boolean running, boolean published, boolean approach, Integer batteryPercent) {
    if (approach) {
      return policy(
          approachIntervalSeconds,
          LocationPriority.HIGH_ACCURACY,
          1,
          LocationMode.APPROACH,
          "Final pickup approach");
    }
    if (running && batteryPercent != null && batteryPercent < 15) {
      return policy(
          10,
          LocationPriority.BALANCED,
          3,
          LocationMode.LOW_BATTERY,
          "Active trip with battery below 15%");
    }
    if (running) {
      return policy(4, LocationPriority.HIGH_ACCURACY, 4, LocationMode.IN_TRIP, "Active trip");
    }
    if (published) {
      return policy(
          30,
          LocationPriority.BALANCED,
          2,
          LocationMode.PUBLISHED,
          "Published trip awaiting start");
    }
    return policy(0, LocationPriority.NONE, 0, LocationMode.IDLE, "No active or published trip");
  }

  private LocationPolicyResponse policy(
      int seconds, LocationPriority priority, int batchSize, LocationMode mode, String reason) {
    return new LocationPolicyResponse(seconds, priority, batchSize, mode, reason);
  }
}
