package com.routeshare.location.cache;

import java.time.Duration;
import java.util.Optional;

public interface LatestLocationCache {
  Duration ttl();

  Optional<LocationSnapshot> findByTripId(Long tripId);

  void put(LocationSnapshot snapshot);
}
