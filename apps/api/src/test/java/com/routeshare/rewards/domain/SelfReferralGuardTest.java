package com.routeshare.rewards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SelfReferralGuardTest {
  @Test
  void identityPhoneAndKnownDeviceEachBlockSelfReferral() {
    assertThat(ReferralPolicy.selfReferral(7, 7, false, false)).isTrue();
    assertThat(ReferralPolicy.selfReferral(7, 8, true, false)).isTrue();
    assertThat(ReferralPolicy.selfReferral(7, 8, false, true)).isTrue();
    assertThat(ReferralPolicy.selfReferral(7, 8, false, false)).isFalse();
  }
}
