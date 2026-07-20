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

class RouteMetricsAdapterTest {
  private static final String DISTANCE_MATRIX_OK =
      """
      {"rows":[{"elements":[{"status":"OK","distance":{"value":9500},"duration":{"value":1200}}]}]}
      """;

  @Test
  void fallsBackToHaversineWhenMapsNotConfigured() {
    var props = new GoogleMapsProperties(false, "");
    var adapter =
        new RouteMetricsAdapter(
            props, new ObjectMapper(), HttpClient.newHttpClient(), new InMemoryJsonCache());

    var metrics = adapter.distanceAndDuration(0, 0, 0, 1);

    assertThat(metrics.source()).isEqualTo("HAVERSINE_ESTIMATE");
    // ~111 km straight-line * 1.3 road factor
    assertThat(metrics.distanceMeters()).isBetween(144_000L, 145_000L);
    assertThat(metrics.durationSeconds()).isPositive();
  }

  @Test
  @SuppressWarnings("unchecked")
  void cachesGoogleResultSoRepeatLookupsSkipTheProvider() throws Exception {
    var httpClient = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn(DISTANCE_MATRIX_OK);
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    var cache = new InMemoryJsonCache();
    var adapter =
        new RouteMetricsAdapter(
            new GoogleMapsProperties(true, "test-key"), new ObjectMapper(), httpClient, cache);

    var first = adapter.distanceAndDuration(6.9336, 79.8500, 6.8649, 79.8997);
    var second = adapter.distanceAndDuration(6.9336, 79.8500, 6.8649, 79.8997);

    assertThat(first.distanceMeters()).isEqualTo(9500);
    assertThat(first.source()).isEqualTo("GOOGLE_DISTANCE_MATRIX");
    assertThat(second).isEqualTo(first);
    verify(httpClient, times(1)).send(any(), any(HttpResponse.BodyHandler.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void nearbyCoordinatesShareTheRoundedCacheEntry() throws Exception {
    var httpClient = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn(DISTANCE_MATRIX_OK);
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    var adapter =
        new RouteMetricsAdapter(
            new GoogleMapsProperties(true, "test-key"),
            new ObjectMapper(),
            httpClient,
            new InMemoryJsonCache());

    // ~20 m apart: rounds to the same 3-decimal cache key.
    adapter.distanceAndDuration(6.93360, 79.85000, 6.86490, 79.89970);
    adapter.distanceAndDuration(6.93368, 79.85004, 6.86488, 79.89966);

    verify(httpClient, times(1)).send(any(), any(HttpResponse.BodyHandler.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void repeatedFailuresOpenTheCooldownAndFallBackWithoutCalling() throws Exception {
    var httpClient = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(500);
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    var adapter =
        new RouteMetricsAdapter(
            new GoogleMapsProperties(true, "test-key"),
            new ObjectMapper(),
            httpClient,
            new InMemoryJsonCache());

    // Distinct coordinates so the cache never short-circuits; threshold is 3 failures.
    assertThat(adapter.distanceAndDuration(1, 1, 2, 2).source()).isEqualTo("HAVERSINE_ESTIMATE");
    assertThat(adapter.distanceAndDuration(3, 3, 4, 4).source()).isEqualTo("HAVERSINE_ESTIMATE");
    assertThat(adapter.distanceAndDuration(5, 5, 6, 6).source()).isEqualTo("HAVERSINE_ESTIMATE");
    assertThat(adapter.distanceAndDuration(7, 7, 8, 8).source()).isEqualTo("HAVERSINE_ESTIMATE");

    // Breaker opened after the third failure; the fourth request never reached the provider.
    verify(httpClient, times(3)).send(any(), any(HttpResponse.BodyHandler.class));
  }
}
