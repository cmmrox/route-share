package com.routeshare.maps.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ProviderCooldownTest {

  @Test
  void staysClosedBelowFailureThreshold() {
    var cooldown = new ProviderCooldown(3, Duration.ofSeconds(30));
    cooldown.recordFailure();
    cooldown.recordFailure();
    assertThat(cooldown.isOpen()).isFalse();
  }

  @Test
  void opensAfterConsecutiveFailuresAndClosesAfterCooldown() {
    var now = new AtomicLong(1_000_000L);
    var cooldown = new ProviderCooldown(3, Duration.ofSeconds(30), now::get);

    cooldown.recordFailure();
    cooldown.recordFailure();
    cooldown.recordFailure();
    assertThat(cooldown.isOpen()).isTrue();

    now.addAndGet(29_000);
    assertThat(cooldown.isOpen()).isTrue();

    now.addAndGet(2_000);
    assertThat(cooldown.isOpen()).isFalse();
  }

  @Test
  void successResetsTheFailureStreak() {
    var cooldown = new ProviderCooldown(3, Duration.ofSeconds(30));
    cooldown.recordFailure();
    cooldown.recordFailure();
    cooldown.recordSuccess();
    cooldown.recordFailure();
    cooldown.recordFailure();
    assertThat(cooldown.isOpen()).isFalse();
  }
}
