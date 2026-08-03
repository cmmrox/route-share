package com.routeshare.rewards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RewardsLedgerIdempotencyTest {
  @Test
  void sameEdgeAndBookingAlwaysAddressTheSameLedgerMutation() {
    assertThat(ReferralPolicy.accrualKey(41, 99))
        .isEqualTo(ReferralPolicy.accrualKey(41, 99))
        .isNotEqualTo(ReferralPolicy.accrualKey(41, 100))
        .isNotEqualTo(ReferralPolicy.accrualKey(42, 99));
  }
}
