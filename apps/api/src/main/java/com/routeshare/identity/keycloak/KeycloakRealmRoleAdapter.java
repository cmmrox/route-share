package com.routeshare.identity.keycloak;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.common.security.RouteShareRoles;
import com.routeshare.identity.config.KeycloakAdminProperties;
import com.routeshare.identity.service.KeycloakRealmRoleService;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Reuses the existing project Keycloak admin {@link RestClient} + {@link KeycloakAdminProperties}
 * (no new Keycloak is introduced) to add/remove RouteShare realm roles via the admin REST API.
 * Lives outside {@code service/impl} because it issues HTTP verbs (the persistence-architecture
 * test forbids SQL-keyword tokens in service implementations).
 */
@Component
public class KeycloakRealmRoleAdapter implements KeycloakRealmRoleService {
  private static final Set<String> MANAGED_ROLES =
      Set.of(
          RouteShareRoles.PASSENGER,
          RouteShareRoles.DRIVER,
          RouteShareRoles.ADMIN,
          RouteShareRoles.SUPPORT_AGENT,
          RouteShareRoles.VERIFICATION_AGENT,
          RouteShareRoles.FINANCE_ADMIN,
          RouteShareRoles.OPS_ADMIN,
          RouteShareRoles.SUPER_ADMIN);

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final KeycloakAdminProperties properties;

  public KeycloakRealmRoleAdapter(
      RestClient keycloakAdminRestClient,
      ObjectMapper objectMapper,
      KeycloakAdminProperties properties) {
    this.restClient = keycloakAdminRestClient;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  @Override
  public void setRealmRoles(String keycloakSubject, Set<String> desiredRoles) {
    if (!properties.enabled() || !properties.hasAdminCredentials()) {
      throw new IllegalStateException(
          "Keycloak admin is not configured; cannot update realm roles");
    }
    for (String role : desiredRoles) {
      if (!MANAGED_ROLES.contains(role)) {
        throw new IllegalArgumentException("Unsupported role: " + role);
      }
    }
    String token = adminAccessToken();
    List<Map<String, Object>> toAdd = new ArrayList<>();
    List<Map<String, Object>> toRemove = new ArrayList<>();
    for (String role : MANAGED_ROLES) {
      Map<String, Object> rep = roleRepresentation(role, token);
      if (rep == null) {
        continue; // role not defined in the realm; skip
      }
      if (desiredRoles.contains(role)) {
        toAdd.add(rep);
      } else {
        toRemove.add(rep);
      }
    }
    if (!toAdd.isEmpty()) {
      roleMappings(keycloakSubject, token, HttpMethod.POST, toAdd);
    }
    if (!toRemove.isEmpty()) {
      roleMappings(keycloakSubject, token, HttpMethod.DELETE, toRemove);
    }
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
      throw new IllegalStateException("Keycloak admin token response had no access token");
    }
    return token;
  }

  private Map<String, Object> roleRepresentation(String role, String token) {
    try {
      String response =
          restClient
              .get()
              .uri(path("/admin/realms/" + properties.realm() + "/roles/" + role))
              .header(HttpHeaders.AUTHORIZATION, bearer(token))
              .retrieve()
              .body(String.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> rep = objectMapper.convertValue(parse(response), Map.class);
      return rep;
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 404) {
        return null;
      }
      throw e;
    }
  }

  private void roleMappings(
      String userId, String token, HttpMethod method, List<Map<String, Object>> reps) {
    var uri =
        path("/admin/realms/" + properties.realm() + "/users/" + userId + "/role-mappings/realm");
    restClient
        .method(method)
        .uri(uri)
        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        .contentType(MediaType.APPLICATION_JSON)
        .body(reps)
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
}
