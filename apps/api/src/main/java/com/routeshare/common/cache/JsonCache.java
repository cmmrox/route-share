package com.routeshare.common.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Small JSON value cache for expensive external-provider responses. Implementations must fail open:
 * a cache/store failure is a miss, never an error surfaced to the caller.
 */
public interface JsonCache {
  <T> Optional<T> get(String key, Class<T> type);

  void put(String key, Object value, Duration ttl);
}
