package com.routeshare.location.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LocationPolicyResolverTest {
  @Test
  void servesAllFiveAdaptiveModes() {
    var policies = new LocationPolicyResolver(2);
    assertThat(policies.resolve(false, false, false, 80).mode()).isEqualTo(LocationMode.IDLE);
    assertThat(policies.resolve(false, true, false, 80).mode()).isEqualTo(LocationMode.PUBLISHED);
    assertThat(policies.resolve(true, false, false, 80).mode()).isEqualTo(LocationMode.IN_TRIP);
    assertThat(policies.resolve(true, false, true, 80).mode()).isEqualTo(LocationMode.APPROACH);
    assertThat(policies.resolve(true, false, false, 10).mode()).isEqualTo(LocationMode.LOW_BATTERY);
  }
}
