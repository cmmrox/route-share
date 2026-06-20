package com.routeshare.common.ratelimit;

import java.time.Duration;

/**
 * Fixed-window rate limiter for abuse-sensitive actions (OTP, payment intents, SOS). Throws HTTP
 * 429 when the caller exceeds {@code limit} within {@code window}. Implementations should fail open
 * if the backing store is unavailable, so a Redis outage degrades to "no limiting" rather than an
 * outage of the protected endpoint.
 */
public interface RateLimiter {
  void check(String action, String key, int limit, Duration window);
}
