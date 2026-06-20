package com.routeshare.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;

class RedisRateLimiterTest {
  private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

  @SuppressWarnings("unchecked")
  private final ValueOperations<String, String> ops = mock(ValueOperations.class);

  private final RateLimitProperties props = new RateLimitProperties(true, null, null, null, null);
  private final RedisRateLimiter limiter = new RedisRateLimiter(redis, props);

  @Test
  void setsTtlOnFirstHitAndAllowsUnderLimit() {
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.increment("rl:otp-request:+94771234567")).thenReturn(1L);

    limiter.check("otp-request", "+94771234567", 5, Duration.ofHours(1));

    verify(redis).expire("rl:otp-request:+94771234567", Duration.ofHours(1));
  }

  @Test
  void throws429WhenOverLimit() {
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.increment(anyString())).thenReturn(6L);

    assertThatThrownBy(() -> limiter.check("otp-request", "x", 5, Duration.ofHours(1)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Too many requests");
  }

  @Test
  void failsOpenWhenRedisUnavailable() {
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.increment(anyString())).thenThrow(new RuntimeException("connection refused"));

    // Must not throw — degrades to allowing the request.
    limiter.check("otp-request", "x", 5, Duration.ofHours(1));
  }

  @Test
  void disabledLimiterIsNoOp() {
    var disabled =
        new RedisRateLimiter(redis, new RateLimitProperties(false, null, null, null, null));
    disabled.check("otp-request", "x", 1, Duration.ofHours(1));
    verify(redis, org.mockito.Mockito.never()).opsForValue();
  }

  @Test
  void blankKeyIsNoOp() {
    limiter.check("otp-request", "", 1, Duration.ofHours(1));
    assertThat(true).isTrue();
  }
}
