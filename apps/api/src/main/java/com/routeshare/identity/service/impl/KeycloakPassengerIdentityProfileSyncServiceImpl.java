package com.routeshare.identity.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.identity.config.KeycloakAdminProperties;
import com.routeshare.identity.service.PassengerIdentityProfileSyncService;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class KeycloakPassengerIdentityProfileSyncServiceImpl
    implements PassengerIdentityProfileSyncService {
  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final KeycloakAdminProperties properties;

  public KeycloakPassengerIdentityProfileSyncServiceImpl(
      RestClient keycloakAdminRestClient,
      ObjectMapper objectMapper,
      KeycloakAdminProperties properties) {
    this.restClient = keycloakAdminRestClient;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  @Override
  public void syncPassengerProfile(
      String keycloakSubject, String fullName, String photoUrl, Map<String, Object> preferences) {
    if (!properties.enabled() || keycloakSubject == null || keycloakSubject.startsWith("phone:")) {
      return;
    }
    if (!properties.hasAdminCredentials()) {
      throw new IllegalStateException("Keycloak admin credentials are not configured");
    }
    String token = adminAccessToken();
    JsonNode existing = getUser(keycloakSubject, token);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("enabled", existing.path("enabled").asBoolean(true));
    body.put("firstName", firstName(fullName));
    body.put("lastName", lastName(fullName));
    String email = stringPreference(preferences, "email");
    if (!email.isBlank()) {
      body.put("email", email);
      body.put("emailVerified", false);
    }
    body.put(
        "attributes",
        mergedAttributes(existing.path("attributes"), fullName, photoUrl, preferences));

    restClient
        .put()
        .uri(path("/admin/realms/" + properties.realm() + "/users/" + keycloakSubject))
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .toBodilessEntity();
  }

  private Map<String, List<String>> mergedAttributes(
      JsonNode existingAttributes,
      String fullName,
      String photoUrl,
      Map<String, Object> preferences) {
    Map<String, List<String>> attributes = new LinkedHashMap<>();
    if (existingAttributes.isObject()) {
      existingAttributes
          .fields()
          .forEachRemaining(
              entry -> {
                if (entry.getValue().isArray()) {
                  attributes.put(
                      entry.getKey(),
                      objectMapper.convertValue(
                          entry.getValue(),
                          objectMapper
                              .getTypeFactory()
                              .constructCollectionType(List.class, String.class)));
                }
              });
    }
    putIfNotBlank(attributes, "route_share_full_name", fullName);
    putIfNotBlank(attributes, "route_share_photo_url", photoUrl);
    putIfNotBlank(
        attributes, "route_share_referral_code", stringPreference(preferences, "referralCode"));
    return attributes;
  }

  private String adminAccessToken() {
    var form = new org.springframework.util.LinkedMultiValueMap<String, String>();
    form.add("client_id", properties.clientId());
    form.add("username", properties.username());
    form.add("password", properties.password());
    form.add("grant_type", "password");
    String response =
        restClient
            .post()
            .uri(path("/realms/" + properties.adminRealm() + "/protocol/openid-connect/token"))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(String.class);
    String token = parse(response).path("access_token").asText();
    if (token.isBlank()) {
      throw new IllegalStateException(
          "Keycloak admin token response did not include an access token");
    }
    return token;
  }

  private JsonNode getUser(String userId, String token) {
    String response =
        restClient
            .get()
            .uri(path("/admin/realms/" + properties.realm() + "/users/" + userId))
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .retrieve()
            .body(String.class);
    return parse(response);
  }

  private JsonNode parse(String response) {
    try {
      return objectMapper.readTree(response == null ? "" : response);
    } catch (IOException e) {
      throw new IllegalStateException("Keycloak returned malformed JSON", e);
    }
  }

  private void putIfNotBlank(Map<String, List<String>> attributes, String key, String value) {
    if (value != null && !value.isBlank()) {
      attributes.put(key, List.of(value));
    }
  }

  private String stringPreference(Map<String, Object> preferences, String key) {
    Object value = preferences == null ? null : preferences.get(key);
    return value instanceof String text ? text.trim() : "";
  }

  private String firstName(String fullName) {
    String[] parts = cleanName(fullName).split(" ", 2);
    return parts.length == 0 ? "" : parts[0];
  }

  private String lastName(String fullName) {
    String[] parts = cleanName(fullName).split(" ", 2);
    return parts.length < 2 ? "" : parts[1];
  }

  private String cleanName(String fullName) {
    return fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private URI path(String path) {
    return properties.serverUrl().resolve(path);
  }
}
