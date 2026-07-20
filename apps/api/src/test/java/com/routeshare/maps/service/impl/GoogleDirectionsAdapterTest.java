package com.routeshare.maps.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.cache.InMemoryJsonCache;
import com.routeshare.maps.config.GoogleMapsProperties;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
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
        new GoogleDirectionsAdapter(
            new GoogleMapsProperties(false, ""), new ObjectMapper(), new InMemoryJsonCache());
    var result = adapter.route(6.9336, 79.8500, 6.8649, 79.8997);
    assertThat(result.source()).isEqualTo("straight_line");
    assertThat(result.coordinates()).hasSize(2);
    assertThat(result.coordinates().get(0).latitude()).isEqualTo(6.9336);
    assertThat(result.coordinates().get(1).longitude()).isEqualTo(79.8997);
  }

  @Test
  @SuppressWarnings("unchecked")
  void cachesGoogleRouteSoRepeatViewsSkipTheProvider() throws Exception {
    String directionsOk =
        """
        {"status":"OK","routes":[{"overview_polyline":{"points":"_p~iF~ps|U_ulLnnqC_mqNvxq`@"},
          "legs":[{"distance":{"value":8200},"duration":{"value":1100}}]}]}
        """;
    var httpClient = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn(directionsOk);
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    var cache = new InMemoryJsonCache();
    var adapter =
        new GoogleDirectionsAdapter(
            new GoogleMapsProperties(true, "test-key"), new ObjectMapper(), httpClient, cache);

    var first = adapter.route(6.9336, 79.8500, 6.8649, 79.8997);
    var second = adapter.route(6.9336, 79.8500, 6.8649, 79.8997);

    assertThat(first.source()).isEqualTo("google_directions");
    assertThat(first.coordinates()).hasSize(3);
    assertThat(second).isEqualTo(first);
    verify(httpClient, times(1)).send(any(), any(HttpResponse.BodyHandler.class));
  }
}
