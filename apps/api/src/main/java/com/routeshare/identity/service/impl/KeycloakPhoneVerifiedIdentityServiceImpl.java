package com.routeshare.identity.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.identity.config.KeycloakAdminProperties;
import com.routeshare.identity.service.PhoneVerifiedIdentityService;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class KeycloakPhoneVerifiedIdentityServiceImpl implements PhoneVerifiedIdentityService {
  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final KeycloakAdminProperties properties;

  public KeycloakPhoneVerifiedIdentityServiceImpl(
      RestClient keycloakAdminRestClient,
      ObjectMapper objectMapper,
      KeycloakAdminProperties properties) {
    this.restClient = keycloakAdminRestClient;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  @Override
  public VerifiedPhoneUser ensurePassengerUser(String phoneE164) {
    if (!properties.enabled()) {
      throw new IllegalStateException(
          "Keycloak user sync is disabled; phone OTP cannot create users");
    }
    if (!properties.hasAdminCredentials()) {
      throw new IllegalStateException("Keycloak admin credentials are not configured");
    }

    String token = adminAccessToken();
    String userId = findUserIdByUsername(phoneE164, token);
    if (userId == null) {
      userId = createPhoneUser(phoneE164, token);
    }
    assignPassengerRole(userId, token);
    return new VerifiedPhoneUser(userId, phoneE164, phoneE164, Set.of(properties.passengerRole()));
  }

  private String adminAccessToken() {
    var form = new LinkedMultiValueMap<String, String>();
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

  private String findUserIdByUsername(String username, String token) {
    String response =
        restClient
            .get()
            .uri(
                builder ->
                    builder
                        .scheme(properties.serverUrl().getScheme())
                        .host(properties.serverUrl().getHost())
                        .port(resolvePort(properties.serverUrl()))
                        .path("/admin/realms/" + properties.realm() + "/users")
                        .queryParam("username", username)
                        .queryParam("exact", true)
                        .build())
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .retrieve()
            .body(String.class);
    JsonNode users = parse(response);
    if (users.isArray() && !users.isEmpty()) {
      return users.get(0).path("id").asText(null);
    }
    return null;
  }

  private String createPhoneUser(String phoneE164, String token) {
    var body =
        Map.of(
            "username",
            phoneE164,
            "enabled",
            true,
            "emailVerified",
            false,
            "attributes",
            Map.of("phone_number", List.of(phoneE164), "phone_verified", List.of("true")));
    try {
      var response =
          restClient
              .post()
              .uri(path("/admin/realms/" + properties.realm() + "/users"))
              .header(HttpHeaders.AUTHORIZATION, bearer(token))
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .toBodilessEntity();
      URI location = response.getHeaders().getLocation();
      if (location != null) {
        String path = location.getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
          return path.substring(lastSlash + 1);
        }
      }
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() != 409) {
        throw e;
      }
    }

    String created = findUserIdByUsername(phoneE164, token);
    if (created == null || created.isBlank()) {
      throw new IllegalStateException("Keycloak user was created but could not be resolved");
    }
    return created;
  }

  private void assignPassengerRole(String userId, String token) {
    String response =
        restClient
            .get()
            .uri(
                path(
                    "/admin/realms/" + properties.realm() + "/roles/" + properties.passengerRole()))
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .retrieve()
            .body(String.class);
    JsonNode role = parse(response);
    restClient
        .post()
        .uri(
            path(
                "/admin/realms/"
                    + properties.realm()
                    + "/users/"
                    + userId
                    + "/role-mappings/realm"))
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(List.of(objectMapper.convertValue(role, Map.class)))
        .retrieve()
        .toBodilessEntity();
  }

  private JsonNode parse(String response) {
    try {
      return objectMapper.readTree(response == null ? "" : response);
    } catch (IOException e) {
      throw new IllegalStateException("Keycloak returned malformed JSON", e);
    }
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private URI path(String path) {
    return properties.serverUrl().resolve(path);
  }

  private int resolvePort(URI uri) {
    return uri.getPort() == -1 ? -1 : uri.getPort();
  }
}
