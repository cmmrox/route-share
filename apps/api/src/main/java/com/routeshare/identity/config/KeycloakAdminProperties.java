package com.routeshare.identity.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "routeshare.keycloak-admin")
public record KeycloakAdminProperties(
    boolean enabled,
    URI serverUrl,
    String realm,
    String adminRealm,
    String clientId,
    String username,
    String password,
    String passengerRole) {
  public KeycloakAdminProperties {
    serverUrl = serverUrl == null ? URI.create("http://localhost:8081") : serverUrl;
    realm = blankToDefault(realm, "routeshare");
    adminRealm = blankToDefault(adminRealm, "master");
    clientId = blankToDefault(clientId, "admin-cli");
    username = blankToDefault(username, "admin");
    password = password == null ? "" : password;
    passengerRole = blankToDefault(passengerRole, "PASSENGER");
  }

  public boolean hasAdminCredentials() {
    return !username.isBlank() && !password.isBlank();
  }

  private static String blankToDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
