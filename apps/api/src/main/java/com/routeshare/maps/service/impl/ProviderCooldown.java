package com.routeshare.maps.service.impl;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Tiny in-process circuit breaker for outbound Google Maps calls: after {@code failureThreshold}
 * consecutive failures the provider is skipped for {@code cooldown}, so a Google brownout degrades
 * to the local fallback immediately instead of holding a request thread for the full HTTP timeout
 * on every call.
 */
public final class ProviderCooldown {
  private final int failureThreshold;
  private final long cooldownMillis;
  private final LongSupplier nowMillis;
  private final AtomicInteger consecutiveFailures = new AtomicInteger();
  private volatile long openUntilMillis;

  public ProviderCooldown(int failureThreshold, Duration cooldown) {
    this(failureThreshold, cooldown, System::currentTimeMillis);
  }

  ProviderCooldown(int failureThreshold, Duration cooldown, LongSupplier nowMillis) {
    this.failureThreshold = Math.max(1, failureThreshold);
    this.cooldownMillis = Math.max(0, cooldown.toMillis());
    this.nowMillis = nowMillis;
  }

  /** True while the breaker is open and the provider should be skipped. */
  public boolean isOpen() {
    return nowMillis.getAsLong() < openUntilMillis;
  }

  public void recordSuccess() {
    consecutiveFailures.set(0);
  }

  public void recordFailure() {
    if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
      openUntilMillis = nowMillis.getAsLong() + cooldownMillis;
      consecutiveFailures.set(0);
    }
  }
}
