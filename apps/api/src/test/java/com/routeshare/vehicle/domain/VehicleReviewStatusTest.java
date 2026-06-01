package com.routeshare.vehicle.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class VehicleReviewStatusTest {
  @Test
  void adminReviewOnlyAllowsTerminalReviewStatuses() {
    assertThat(Arrays.stream(VehicleReviewStatus.values()).map(Enum::name))
        .containsExactly("APPROVED", "REJECTED", "SUSPENDED");
  }
}
