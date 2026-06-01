package com.routeshare.location.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisLatestLocationCache implements LatestLocationCache {
  private static final String KEY_PREFIX = "routeshare:trip:latest-location:";
  private static final Duration TTL = Duration.ofSeconds(30);

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  @Override
  public Duration ttl() {
    return TTL;
  }

  @Override
  public Optional<LocationSnapshot> findByTripId(Long tripId) {
    try {
      String payload = redis.opsForValue().get(KEY_PREFIX + tripId);
      if (payload == null || payload.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.readValue(payload, LocationSnapshot.class));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to read latest trip location from Redis", ex);
    }
  }

  @Override
  public void put(LocationSnapshot snapshot) {
    try {
      redis
          .opsForValue()
          .set(KEY_PREFIX + snapshot.tripId(), objectMapper.writeValueAsString(snapshot), TTL);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to cache latest trip location in Redis", ex);
    }
  }
}
