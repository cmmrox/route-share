package com.routeshare.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Test double: JSON round-trips like the Redis cache, but stores in-process (TTL ignored). */
public class InMemoryJsonCache implements JsonCache {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<String, String> store = new ConcurrentHashMap<>();

  @Override
  public <T> Optional<T> get(String key, Class<T> type) {
    String json = store.get(key);
    if (json == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(json, type));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public void put(String key, Object value, Duration ttl) {
    try {
      store.put(key, objectMapper.writeValueAsString(value));
    } catch (Exception e) {
      // test double: ignore
    }
  }

  public int size() {
    return store.size();
  }

  public boolean containsKeyWithPrefix(String prefix) {
    return store.keySet().stream().anyMatch(k -> k.startsWith(prefix));
  }
}
