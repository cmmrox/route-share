package com.routeshare.location.cache;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryLatestLocationCache implements LatestLocationCache {
  private final Map<Long, LocationSnapshot> snapshots = new ConcurrentHashMap<>();
  private final Duration ttl;

  public InMemoryLatestLocationCache() {
    this(Duration.ofSeconds(30));
  }

  public InMemoryLatestLocationCache(Duration ttl) {
    this.ttl = ttl;
  }

  @Override
  public Duration ttl() {
    return ttl;
  }

  @Override
  public Optional<LocationSnapshot> findByTripId(Long tripId) {
    return Optional.ofNullable(snapshots.get(tripId));
  }

  @Override
  public void put(LocationSnapshot snapshot) {
    snapshots.put(snapshot.tripId(), snapshot);
  }
}
