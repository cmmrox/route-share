package com.routeshare.rewards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReferralWindowBoundsTest {
  private static final Instant NOW = Instant.parse("2026-08-03T12:00:00Z");

  @Test
  void whicheverBoundEndsFirstStopsAccrualAtTheBoundary() {
    assertThat(ReferralPolicy.edgeCanAccrue("ACTIVE", NOW.plusSeconds(1), 49, 50, NOW)).isTrue();
    assertThat(ReferralPolicy.edgeCanAccrue("ACTIVE", NOW.plusSeconds(1), 50, 50, NOW)).isFalse();
    assertThat(ReferralPolicy.edgeCanAccrue("ACTIVE", NOW, 0, 50, NOW)).isFalse();
    assertThat(ReferralPolicy.edgeCanAccrue("EXPIRED_WINDOW", NOW.plusSeconds(1), 0, 50, NOW))
        .isFalse();
  }
}
