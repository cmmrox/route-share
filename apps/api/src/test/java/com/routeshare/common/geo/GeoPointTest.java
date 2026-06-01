package com.routeshare.common.geo;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GeoPointTest {
  @Test
  void acceptsValidSriLankaCoordinate() {
    var p = new GeoPoint(6.9271, 79.8612);
    assertThat(p.latitude()).isEqualTo(6.9271);
  }

  @Test
  void rejectsInvalidLatitude() {
    assertThatThrownBy(() -> new GeoPoint(100, 79)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsInvalidLongitude() {
    assertThatThrownBy(() -> new GeoPoint(6, 190)).isInstanceOf(IllegalArgumentException.class);
  }
}
