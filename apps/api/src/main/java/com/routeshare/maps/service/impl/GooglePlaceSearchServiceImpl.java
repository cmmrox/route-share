package com.routeshare.maps.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GooglePlaceSearchServiceImpl implements PlaceSearchService {
  private static final URI AUTOCOMPLETE_URI =
      URI.create("https://places.googleapis.com/v1/places:autocomplete");
  private static final String DETAILS_URL = "https://places.googleapis.com/v1/places/";

  private final GoogleMapsProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  @Autowired
  public GooglePlaceSearchServiceImpl(GoogleMapsProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, HttpClient.newHttpClient());
  }

  GooglePlaceSearchServiceImpl(
      GoogleMapsProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
  }

  @Override
  public List<PlaceSuggestionResponse> autocomplete(
      String query, Double latitude, Double longitude) {
    ensureReady();
    if (query == null || query.trim().length() < 2) return List.of();
    try {
      Map<String, Object> body = autocompleteBody(query, latitude, longitude);
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
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Google Places autocomplete is unavailable. Retry later.");
    }
  }

  @Override
  public PlaceSuggestionResponse details(String placeId) {
    ensureReady();
    if (placeId == null || placeId.isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Place id is required.");
    try {
      String encodedPlaceId = URLEncoder.encode(placeId, StandardCharsets.UTF_8);
      URI uri = URI.create(DETAILS_URL + encodedPlaceId);
      HttpRequest request =
          HttpRequest.newBuilder(uri)
              .timeout(Duration.ofSeconds(8))
              .header("X-Goog-Api-Key", properties.serverApiKey())
              .header("X-Goog-FieldMask", "id,displayName,formattedAddress,location")
              .GET()
              .build();
      JsonNode root = send(request);
      JsonNode location = root.path("location");
      return new PlaceSuggestionResponse(
          root.path("id").asText(placeId),
          root.path("displayName")
              .path("text")
              .asText(root.path("formattedAddress").asText("Selected place")),
          root.path("formattedAddress").asText(""),
          new CoordinateResponse(
              location.path("latitude").asDouble(), location.path("longitude").asDouble()));
    } catch (IOException | InterruptedException e) {
      if (e instanceof InterruptedException) Thread.currentThread().interrupt();
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Google Place details are unavailable. Retry later.");
    }
  }

  private Map<String, Object> autocompleteBody(String query, Double latitude, Double longitude) {
    if (latitude != null && longitude != null) {
      return Map.of(
          "input",
          query.trim(),
          "includedRegionCodes",
          List.of("lk"),
          "locationBias",
          Map.of(
              "circle",
              Map.of(
                  "center",
                  Map.of("latitude", latitude, "longitude", longitude),
                  "radius",
                  50000.0)));
    }
    return Map.of("input", query.trim(), "includedRegionCodes", List.of("lk"));
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
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Google Maps Platform rejected the place request.");
    }
    return objectMapper.readTree(response.body());
  }

  private void ensureReady() {
    if (!properties.ready()) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_FAILED, "Google Maps Platform is not configured.");
    }
  }
}
