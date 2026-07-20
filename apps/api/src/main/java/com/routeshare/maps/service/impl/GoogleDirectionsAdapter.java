package com.routeshare.maps.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.cache.JsonCache;
import com.routeshare.maps.config.GoogleMapsProperties;
import com.routeshare.maps.dto.CoordinateResponse;
import com.routeshare.maps.service.DirectionsPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Road-following route via the Google Directions API when maps are configured; otherwise a straight
 * two-point line. Never throws on provider failure — it degrades to the straight line so the map
 * always has a polyline to render.
 *
 * <p>Cost controls: routes are cached in Redis keyed by coordinates rounded to ~11 m (map display
 * precision), and a cooldown breaker skips Google after repeated failures. Matched-ride maps should
 * prefer the stored route geometry endpoint, which never calls Google.
 */
@Service
public class GoogleDirectionsAdapter implements DirectionsPort {
  private static final Logger log = LoggerFactory.getLogger(GoogleDirectionsAdapter.class);
  private static final String BASE = "https://maps.googleapis.com/maps/api/directions/json";
  private static final String CACHE_PREFIX = "maps:dir:";

  private final GoogleMapsProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final JsonCache cache;
  private final ProviderCooldown cooldown;

  @Autowired
  public GoogleDirectionsAdapter(
      GoogleMapsProperties properties, ObjectMapper objectMapper, JsonCache cache) {
    this(properties, objectMapper, HttpClient.newHttpClient(), cache);
  }

  GoogleDirectionsAdapter(
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
  public DirectionsResult route(
      double originLat, double originLng, double destLat, double destLng) {
    if (properties.ready()) {
      String cacheKey = cacheKey(originLat, originLng, destLat, destLng);
      var cached = cache.get(cacheKey, DirectionsResult.class);
      if (cached.isPresent()) {
        return cached.get();
      }
      if (!cooldown.isOpen()) {
        try {
          DirectionsResult result = googleRoute(originLat, originLng, destLat, destLng);
          cooldown.recordSuccess();
          cache.put(cacheKey, result, Duration.ofSeconds(properties.routeCacheTtlSeconds()));
          return result;
        } catch (RuntimeException | java.io.IOException e) {
          cooldown.recordFailure();
          log.warn("directions_failed, falling back to straight line: {}", e.getMessage());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
    return straightLine(originLat, originLng, destLat, destLng);
  }

  private static String cacheKey(double oLat, double oLng, double dLat, double dLng) {
    return CACHE_PREFIX + String.format(Locale.ROOT, "%.4f,%.4f:%.4f,%.4f", oLat, oLng, dLat, dLng);
  }

  private DirectionsResult googleRoute(double oLat, double oLng, double dLat, double dLng)
      throws java.io.IOException, InterruptedException {
    String uri =
        BASE
            + "?origin="
            + oLat
            + ","
            + oLng
            + "&destination="
            + dLat
            + ","
            + dLng
            + "&mode=driving&key="
            + properties.serverApiKey();
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(8)).GET().build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException("Directions HTTP " + response.statusCode());
    }
    JsonNode root = objectMapper.readTree(response.body());
    String status = root.path("status").asText();
    if (!"OK".equals(status)) {
      throw new IllegalStateException("Directions status " + status);
    }
    JsonNode route = root.path("routes").path(0);
    String encoded = route.path("overview_polyline").path("points").asText();
    List<CoordinateResponse> coordinates = decodePolyline(encoded);
    if (coordinates.size() < 2) {
      throw new IllegalStateException("Directions returned no polyline");
    }
    JsonNode leg = route.path("legs").path(0);
    long distanceMeters = leg.path("distance").path("value").asLong();
    long durationSeconds = leg.path("duration").path("value").asLong();
    return new DirectionsResult(coordinates, distanceMeters, durationSeconds, "google_directions");
  }

  private DirectionsResult straightLine(double oLat, double oLng, double dLat, double dLng) {
    List<CoordinateResponse> coordinates =
        List.of(new CoordinateResponse(oLat, oLng), new CoordinateResponse(dLat, dLng));
    return new DirectionsResult(coordinates, 0, 0, "straight_line");
  }

  /** Decodes a Google encoded polyline string into lat/lng coordinates. */
  static List<CoordinateResponse> decodePolyline(String encoded) {
    List<CoordinateResponse> path = new ArrayList<>();
    if (encoded == null || encoded.isEmpty()) {
      return path;
    }
    int index = 0;
    int lat = 0;
    int lng = 0;
    int len = encoded.length();
    while (index < len) {
      int result = 1;
      int shift = 0;
      int b;
      do {
        b = encoded.charAt(index++) - 63 - 1;
        result += b << shift;
        shift += 5;
      } while (b >= 0x1f && index < len);
      lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

      result = 1;
      shift = 0;
      do {
        b = encoded.charAt(index++) - 63 - 1;
        result += b << shift;
        shift += 5;
      } while (b >= 0x1f && index < len);
      lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

      path.add(new CoordinateResponse(lat * 1e-5, lng * 1e-5));
    }
    return path;
  }
}
