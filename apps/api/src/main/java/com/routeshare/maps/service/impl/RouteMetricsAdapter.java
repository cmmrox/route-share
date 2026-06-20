package com.routeshare.maps.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.maps.config.GoogleMapsProperties;
import com.routeshare.maps.domain.HaversineDistance;
import com.routeshare.maps.service.RouteMetricsPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Distance + duration via Google Distance Matrix when maps are configured; otherwise a great-circle
 * estimate (×1.3 road factor, ~30 km/h) so fares are still computed server-side rather than
 * trusting the client. Never throws on provider failure — it degrades to the offline estimate.
 */
@Service
public class RouteMetricsAdapter implements RouteMetricsPort {
  private static final Logger log = LoggerFactory.getLogger(RouteMetricsAdapter.class);
  private static final String BASE = "https://maps.googleapis.com/maps/api/distancematrix/json";
  private static final double ROAD_FACTOR = 1.3;
  private static final double URBAN_SPEED_MPS = 8.33; // ~30 km/h

  private final GoogleMapsProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  @Autowired
  public RouteMetricsAdapter(GoogleMapsProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, HttpClient.newHttpClient());
  }

  RouteMetricsAdapter(
      GoogleMapsProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
  }

  @Override
  public RouteMetrics distanceAndDuration(
      double originLat, double originLng, double destLat, double destLng) {
    if (properties.ready()) {
      try {
        return googleMetrics(originLat, originLng, destLat, destLng);
      } catch (RuntimeException | java.io.IOException e) {
        log.warn("distance_matrix_failed, falling back to haversine: {}", e.getMessage());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return haversineMetrics(originLat, originLng, destLat, destLng);
  }

  private RouteMetrics googleMetrics(double oLat, double oLng, double dLat, double dLng)
      throws java.io.IOException, InterruptedException {
    String uri =
        BASE
            + "?origins="
            + oLat
            + ","
            + oLng
            + "&destinations="
            + dLat
            + ","
            + dLng
            + "&mode=driving&key="
            + properties.serverApiKey();
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(8)).GET().build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException("Distance Matrix HTTP " + response.statusCode());
    }
    JsonNode element =
        objectMapper.readTree(response.body()).path("rows").path(0).path("elements").path(0);
    if (!"OK".equals(element.path("status").asText())) {
      throw new IllegalStateException(
          "Distance Matrix element status " + element.path("status").asText());
    }
    return new RouteMetrics(
        element.path("distance").path("value").asLong(),
        element.path("duration").path("value").asLong(),
        "GOOGLE_DISTANCE_MATRIX");
  }

  private RouteMetrics haversineMetrics(double oLat, double oLng, double dLat, double dLng) {
    long straight = HaversineDistance.meters(oLat, oLng, dLat, dLng);
    long roadMeters = Math.round(straight * ROAD_FACTOR);
    long durationSeconds = Math.round(roadMeters / URBAN_SPEED_MPS);
    return new RouteMetrics(roadMeters, durationSeconds, "HAVERSINE_ESTIMATE");
  }
}
