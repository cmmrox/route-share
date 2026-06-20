package com.routeshare.maps.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.maps.config.GoogleMapsProperties;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;

class RouteMetricsAdapterTest {
  @Test
  void fallsBackToHaversineWhenMapsNotConfigured() {
    var props = new GoogleMapsProperties(false, "");
    var adapter = new RouteMetricsAdapter(props, new ObjectMapper(), HttpClient.newHttpClient());

    var metrics = adapter.distanceAndDuration(0, 0, 0, 1);

    assertThat(metrics.source()).isEqualTo("HAVERSINE_ESTIMATE");
    // ~111 km straight-line * 1.3 road factor
    assertThat(metrics.distanceMeters()).isBetween(144_000L, 145_000L);
    assertThat(metrics.durationSeconds()).isPositive();
  }
}
