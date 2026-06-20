package com.routeshare.common.ratelimit;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Redis fixed-window limiter: {@code INCR} a per-action+key counter and set the TTL on first hit.
 * Fails open on any Redis error so the protected endpoint stays available.
 */
@Component
public class RedisRateLimiter implements RateLimiter {
  private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

  private final StringRedisTemplate redis;
  private final RateLimitProperties properties;

  public RedisRateLimiter(StringRedisTemplate redis, RateLimitProperties properties) {
    this.redis = redis;
    this.properties = properties;
  }

  @Override
  public void check(String action, String key, int limit, Duration window) {
    if (!properties.enabled() || key == null || key.isBlank()) {
      return;
    }
    String redisKey = "rl:" + action + ":" + key;
    long count;
    try {
      Long incremented = redis.opsForValue().increment(redisKey);
      count = incremented == null ? 1L : incremented;
      if (count == 1L) {
        redis.expire(redisKey, window);
      }
    } catch (RuntimeException e) {
      // Fail open: never block a legitimate request because the limiter store is down.
      log.warn("rate_limit_store_unavailable action={} : {}", action, e.getMessage());
      return;
    }
    if (count > limit) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later.");
    }
  }
}
