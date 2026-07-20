package com.routeshare.maps.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.cache.JsonCache;
import com.routeshare.maps.config.GoogleMapsProperties;
import com.routeshare.maps.dto.CoordinateResponse;
import com.routeshare.maps.dto.PlaceSuggestionResponse;
import com.routeshare.maps.service.PlaceSearchService;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Google Places (New) proxy tuned for cost: autocomplete/details carry the client session token so
 * Google bills them as one session, the details field mask stays on Essentials-tier fields only
 * (displayName is Pro-tier and the client already has the label from the suggestion), and resolved
 * details are cached by place id so repeated selections of popular places are served without a
 * billable call.
 */
@Service
public class GooglePlaceSearchServiceImpl implements PlaceSearchService {
  private static final URI AUTOCOMPLETE_URI =
      URI.create("https://places.googleapis.com/v1/places:autocomplete");
  private static final String DETAILS_URL = "https://places.googleapis.com/v1/places/";
  private static final String DETAILS_FIELD_MASK = "id,formattedAddress,location";
  private static final String DETAILS_CACHE_PREFIX = "maps:place:";

  private final GoogleMapsProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final JsonCache cache;
  private final ProviderCooldown cooldown;

  @Autowired
  public GooglePlaceSearchServiceImpl(
      GoogleMapsProperties properties, ObjectMapper objectMapper, JsonCache cache) {
    this(properties, objectMapper, HttpClient.newHttpClient(), cache);
  }

  GooglePlaceSearchServiceImpl(
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
  public List<PlaceSuggestionResponse> autocomplete(
      String query, Double latitude, Double longitude, String sessionToken) {
    ensureReady();
    if (query == null || query.trim().length() < 2) return List.of();
    ensureProviderAvailable();
    try {
      Map<String, Object> body = autocompleteBody(query, latitude, longitude, sessionToken);
      HttpRequest request =
          HttpRequest.newBuilder(AUTOCOMPLETE_URI)
              .timeout(Duration.ofSeconds(8))
              .header("Content-Type", "application/json")
              .header("X-Goog-Api-Key", properties.serverApiKey())
              .header(
                  "X-Goog-FieldMask",
                  "suggestions.placePrediction.placeId,suggestions.placePrediction.text,suggestions.placePrediction.structuredFormat")
              .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
              .build();
      return parseAutocomplete(send(request));
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      cooldown.recordFailure();
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Google Places autocomplete is unavailable. Retry later.");
    }
  }

  @Override
  public PlaceSuggestionResponse details(String placeId, String sessionToken) {
    ensureReady();
    if (placeId == null || placeId.isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Place id is required.");
    String cacheKey = DETAILS_CACHE_PREFIX + placeId;
    var cached = cache.get(cacheKey, PlaceSuggestionResponse.class);
    if (cached.isPresent()) {
      return cached.get();
    }
    ensureProviderAvailable();
    try {
      String encodedPlaceId = URLEncoder.encode(placeId, StandardCharsets.UTF_8);
      String query =
          sessionToken == null || sessionToken.isBlank()
              ? ""
              : "?sessionToken=" + URLEncoder.encode(sessionToken, StandardCharsets.UTF_8);
      URI uri = URI.create(DETAILS_URL + encodedPlaceId + query);
      HttpRequest request =
          HttpRequest.newBuilder(uri)
              .timeout(Duration.ofSeconds(8))
              .header("X-Goog-Api-Key", properties.serverApiKey())
              .header("X-Goog-FieldMask", DETAILS_FIELD_MASK)
              .GET()
              .build();
      JsonNode root = send(request);
      JsonNode location = root.path("location");
      String formattedAddress = root.path("formattedAddress").asText("");
      PlaceSuggestionResponse response =
          new PlaceSuggestionResponse(
              root.path("id").asText(placeId),
              formattedAddress.isBlank() ? "Selected place" : formattedAddress,
              formattedAddress,
              new CoordinateResponse(
                  location.path("latitude").asDouble(), location.path("longitude").asDouble()));
      cache.put(cacheKey, response, Duration.ofSeconds(properties.placeDetailsCacheTtlSeconds()));
      return response;
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      cooldown.recordFailure();
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Google Place details are unavailable. Retry later.");
    }
  }

  private Map<String, Object> autocompleteBody(
      String query, Double latitude, Double longitude, String sessionToken) {
    Map<String, Object> body = new HashMap<>();
    body.put("input", query.trim());
    body.put("includedRegionCodes", List.of("lk"));
    if (sessionToken != null && !sessionToken.isBlank()) {
      body.put("sessionToken", sessionToken);
    }
    if (latitude != null && longitude != null) {
      body.put(
          "locationBias",
          Map.of(
              "circle",
              Map.of(
                  "center",
                  Map.of("latitude", latitude, "longitude", longitude),
                  "radius",
                  50000.0)));
    }
    return body;
  }

  private List<PlaceSuggestionResponse> parseAutocomplete(JsonNode root) {
    List<PlaceSuggestionResponse> results = new ArrayList<>();
    for (JsonNode suggestion : root.path("suggestions")) {
      JsonNode prediction = suggestion.path("placePrediction");
      if (prediction.isMissingNode()) continue;
      String placeId = prediction.path("placeId").asText("");
      String label = prediction.path("structuredFormat").path("mainText").path("text").asText("");
      String address =
          prediction.path("structuredFormat").path("secondaryText").path("text").asText("");
      String fullText = prediction.path("text").path("text").asText(label);
      if (label.isBlank()) label = fullText;
      if (!placeId.isBlank() && !label.isBlank()) {
        results.add(
            new PlaceSuggestionResponse(
                placeId, label, address.isBlank() ? fullText : address, null));
      }
    }
    return results;
  }

  private JsonNode send(HttpRequest request) throws IOException, InterruptedException {
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      cooldown.recordFailure();
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Google Maps Platform rejected the place request.");
    }
    cooldown.recordSuccess();
    return objectMapper.readTree(response.body());
  }

  private void ensureReady() {
    if (!properties.ready()) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED, "Google Maps Platform is not configured.");
    }
  }

  private void ensureProviderAvailable() {
    if (cooldown.isOpen()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Google Maps Platform is temporarily unavailable. Retry later.");
    }
  }
}
