package com.routeshare.maps.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HaversineDistanceTest {
  @Test
  void zeroForSamePoint() {
    assertThat(HaversineDistance.meters(6.9271, 79.8612, 6.9271, 79.8612)).isZero();
  }

  @Test
  void oneDegreeOfLongitudeAtEquatorIsAboutOneEleventhOfEarth() {
    long m = HaversineDistance.meters(0, 0, 0, 1);
    assertThat(m).isBetween(111_000L, 111_400L);
  }
}
