package com.routeshare.location.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisLatestLocationCacheTest {
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
  private final ValueOperations<String, String> values = mock(ValueOperations.class);
  private final RedisLatestLocationCache cache =
      new RedisLatestLocationCache(redis, new ObjectMapper().findAndRegisterModules());

  @Test
  void ttlIsThirtySeconds() {
    assertThat(cache.ttl()).hasSeconds(30);
  }

  @Test
  void findByTripIdReturnsEmptyWhenRedisHasNoPayload() {
    when(redis.opsForValue()).thenReturn(values);
    when(values.get("routeshare:trip:latest-location:44")).thenReturn(" ");

    assertThat(cache.findByTripId(44L)).isEmpty();
  }

  @Test
  void findByTripIdDeserializesPayload() {
    when(redis.opsForValue()).thenReturn(values);
    when(values.get("routeshare:trip:latest-location:44"))
        .thenReturn(
            "{\"tripId\":44,\"driverProfileId\":7,\"latitude\":6.9,\"longitude\":79.8,\"accuracyMeters\":10,\"speedMps\":4,\"bearingDegrees\":90,\"deviceRecordedAt\":\"2026-06-02T00:00:00Z\",\"serverReceivedAt\":\"2026-06-02T00:00:01Z\"}");

    var snapshot = cache.findByTripId(44L);

    assertThat(snapshot).isPresent();
    assertThat(snapshot.orElseThrow().driverProfileId()).isEqualTo(7L);
  }

  @Test
  void putSerializesSnapshotWithTtl() {
    when(redis.opsForValue()).thenReturn(values);
    var snapshot =
        new LocationSnapshot(
            44L,
            7L,
            6.9,
            79.8,
            10.0,
            4.0,
            90.0,
            Instant.parse("2026-06-02T00:00:00Z"),
            Instant.parse("2026-06-02T00:00:01Z"));

    cache.put(snapshot);

    verify(values)
        .set(eq("routeshare:trip:latest-location:44"), contains("\"tripId\":44"), eq(cache.ttl()));
  }

  @Test
  void redisReadFailureIsWrapped() {
    when(redis.opsForValue()).thenReturn(values);
    when(values.get(anyString())).thenThrow(new RuntimeException("redis down"));

    assertThatThrownBy(() -> cache.findByTripId(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Unable to read latest trip location from Redis");
  }
}
