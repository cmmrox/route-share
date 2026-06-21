package com.routeshare.maps.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.maps.config.GoogleMapsProperties;
import org.junit.jupiter.api.Test;

class GoogleDirectionsAdapterTest {

  @Test
  void decodesGoogleReferencePolyline() {
    // Canonical example from Google's encoded-polyline documentation.
    var points = GoogleDirectionsAdapter.decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
    assertThat(points).hasSize(3);
    assertThat(points.get(0).latitude()).isCloseTo(38.5, org.assertj.core.data.Offset.offset(1e-4));
    assertThat(points.get(0).longitude())
        .isCloseTo(-120.2, org.assertj.core.data.Offset.offset(1e-4));
    assertThat(points.get(2).latitude())
        .isCloseTo(43.252, org.assertj.core.data.Offset.offset(1e-4));
    assertThat(points.get(2).longitude())
        .isCloseTo(-126.453, org.assertj.core.data.Offset.offset(1e-4));
  }

  @Test
  void decodeEmptyReturnsEmpty() {
    assertThat(GoogleDirectionsAdapter.decodePolyline("")).isEmpty();
    assertThat(GoogleDirectionsAdapter.decodePolyline(null)).isEmpty();
  }

  @Test
  void fallsBackToStraightLineWhenMapsDisabled() {
    var adapter =
        new GoogleDirectionsAdapter(new GoogleMapsProperties(false, ""), new ObjectMapper());
    var result = adapter.route(6.9336, 79.8500, 6.8649, 79.8997);
    assertThat(result.source()).isEqualTo("straight_line");
    assertThat(result.coordinates()).hasSize(2);
    assertThat(result.coordinates().get(0).latitude()).isEqualTo(6.9336);
    assertThat(result.coordinates().get(1).longitude()).isEqualTo(79.8997);
  }
}
