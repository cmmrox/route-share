package com.routeshare.identity.facade;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.identity.domain.AppUser;
import java.util.Set;

public interface IdentityFacade {
  AppUser upsertFromToken(CurrentUser currentUser);

  /** Propagates managed realm roles for a user to the project Keycloak (admin role management). */
  void setRealmRoles(String keycloakSubject, Set<String> roles);
}
