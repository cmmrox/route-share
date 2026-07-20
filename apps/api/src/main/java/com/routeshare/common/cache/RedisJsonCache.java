package com.routeshare.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed {@link JsonCache}. Values are stored as JSON strings with a TTL so provider
 * responses (Google Distance Matrix, Directions, Place Details) are reused instead of re-billed.
 * Every Redis or JSON failure degrades to a cache miss.
 */
@Component
public class RedisJsonCache implements JsonCache {
  private static final Logger log = LoggerFactory.getLogger(RedisJsonCache.class);

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  public RedisJsonCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
    this.redis = redis;
    this.objectMapper = objectMapper;
  }

  @Override
  public <T> Optional<T> get(String key, Class<T> type) {
    try {
      String json = redis.opsForValue().get(key);
      if (json == null || json.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.readValue(json, type));
    } catch (Exception e) {
      log.warn("json_cache_read_failed key={} : {}", key, e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public void put(String key, Object value, Duration ttl) {
    try {
      redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
    } catch (Exception e) {
      log.warn("json_cache_write_failed key={} : {}", key, e.getMessage());
    }
  }
}
