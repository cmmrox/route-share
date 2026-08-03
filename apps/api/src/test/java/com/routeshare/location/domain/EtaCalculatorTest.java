package com.routeshare.location.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EtaCalculatorTest {
  @Test
  void derivesEtaFromGeometryAndObservedSpeedWithCorridorFallback() {
    var eta = new EtaCalculator(22);
    assertThat(eta.etaSeconds(800, 8.0)).isEqualTo(100);
    assertThat(eta.etaSeconds(6_111, null)).isBetween(999L, 1_001L);
    assertThat(EtaCalculator.class.getDeclaredFields())
        .noneMatch(field -> field.getType().getName().contains("maps"));
  }
}
