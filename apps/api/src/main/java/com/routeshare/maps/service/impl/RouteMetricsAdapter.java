package com.routeshare.maps.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.cache.JsonCache;
import com.routeshare.maps.config.GoogleMapsProperties;
import com.routeshare.maps.domain.HaversineDistance;
import com.routeshare.maps.service.RouteMetricsPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Distance + duration via Google Distance Matrix when maps are configured; otherwise a great-circle
 * estimate (×1.3 road factor, ~30 km/h) so fares are still computed server-side rather than
 * trusting the client. Never throws on provider failure — it degrades to the offline estimate.
 *
 * <p>Cost controls: results are cached in Redis keyed by coordinates rounded to ~110 m (fare
 * estimates tolerate that granularity), and a short cooldown breaker skips Google entirely after
 * repeated failures instead of paying the HTTP timeout per request.
 */
@Service
public class RouteMetricsAdapter implements RouteMetricsPort {
  private static final Logger log = LoggerFactory.getLogger(RouteMetricsAdapter.class);
  private static final String BASE = "https://maps.googleapis.com/maps/api/distancematrix/json";
  private static final String CACHE_PREFIX = "maps:dm:";
  private static final double ROAD_FACTOR = 1.3;
  private static final double URBAN_SPEED_MPS = 8.33; // ~30 km/h

  private final GoogleMapsProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final JsonCache cache;
  private final ProviderCooldown cooldown;

  @Autowired
  public RouteMetricsAdapter(
      GoogleMapsProperties properties, ObjectMapper objectMapper, JsonCache cache) {
    this(properties, objectMapper, HttpClient.newHttpClient(), cache);
  }

  RouteMetricsAdapter(
      GoogleMapsProperties properties,
      ObjectMapper objectMapper,
      HttpClient httpClient,
      JsonCache cache) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
    this.cache = cache;
    this.cooldown =
        new ProviderCooldown(
            properties.providerFailureThreshold(),
            Duration.ofSeconds(properties.providerCooldownSeconds()));
  }

  @Override
  public RouteMetrics distanceAndDuration(
      double originLat, double originLng, double destLat, double destLng) {
    if (properties.ready()) {
      String cacheKey = cacheKey(originLat, originLng, destLat, destLng);
      var cached = cache.get(cacheKey, RouteMetrics.class);
      if (cached.isPresent()) {
        return cached.get();
      }
      if (!cooldown.isOpen()) {
        try {
          RouteMetrics metrics = googleMetrics(originLat, originLng, destLat, destLng);
          cooldown.recordSuccess();
          cache.put(cacheKey, metrics, Duration.ofSeconds(properties.routeCacheTtlSeconds()));
          return metrics;
        } catch (RuntimeException | java.io.IOException e) {
          cooldown.recordFailure();
          log.warn("distance_matrix_failed, falling back to haversine: {}", e.getMessage());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
    return haversineMetrics(originLat, originLng, destLat, destLng);
  }

  /** ~110 m grid so nearby pickups share a cached fare-distance instead of re-billing Google. */
  private static String cacheKey(double oLat, double oLng, double dLat, double dLng) {
    return CACHE_PREFIX + String.format(Locale.ROOT, "%.3f,%.3f:%.3f,%.3f", oLat, oLng, dLat, dLng);
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
