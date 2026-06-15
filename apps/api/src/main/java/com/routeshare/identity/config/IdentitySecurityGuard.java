package com.routeshare.identity.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class IdentitySecurityGuard {
  static final String DEFAULT_PHONE_TOKEN_KEY = "routeshare-local-phone-access-token-key-change-me";

  private final Environment environment;
  private final KeycloakAdminProperties keycloakAdmin;
  private final String phoneTokenSigningKey;

  public IdentitySecurityGuard(
      Environment environment,
      KeycloakAdminProperties keycloakAdmin,
      @Value("${routeshare.phone-auth.access-token-signing-key:}") String phoneTokenSigningKey) {
    this.environment = environment;
    this.keycloakAdmin = keycloakAdmin;
    this.phoneTokenSigningKey = phoneTokenSigningKey == null ? "" : phoneTokenSigningKey.trim();
  }

  @PostConstruct
  void validateIdentitySecurityConfiguration() {
    if (isLocalLikeProfile()) {
      return;
    }
    if (phoneTokenSigningKey.isBlank() || DEFAULT_PHONE_TOKEN_KEY.equals(phoneTokenSigningKey)) {
      throw new IllegalStateException(
          "ROUTESHARE_PHONE_AUTH_ACCESS_TOKEN_SIGNING_KEY must be configured outside local/test");
    }
    if (keycloakAdmin.enabled()
        && ("admin".equals(keycloakAdmin.username()) || "admin".equals(keycloakAdmin.password()))) {
      throw new IllegalStateException(
          "Default Keycloak admin credentials may only be used in local/test profiles");
    }
  }

  private boolean isLocalLikeProfile() {
    String[] activeProfiles = environment.getActiveProfiles();
    if (activeProfiles.length == 0) {
      return true;
    }
    return Arrays.stream(activeProfiles)
        .anyMatch(
            profile -> profile.equals("local") || profile.equals("dev") || profile.equals("test"));
  }
}
