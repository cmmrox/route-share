package com.routeshare.identity.facade;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.identity.domain.AppUser;
import java.util.Set;

public interface IdentityFacade {
  /**
   * Resolves the local app user for the authenticated token. The projection is cached briefly per
   * subject, so unchanged tokens do not re-write {@code identity.app_user} on every request.
   */
  AppUser upsertFromToken(CurrentUser currentUser);

  /** Propagates managed realm roles for a user to the project Keycloak (admin role management). */
  void setRealmRoles(String keycloakSubject, Set<String> roles);

  /**
   * Drops the cached projection for a subject so status changes (suspend/activate) are enforced on
   * the user's next request instead of after cache expiry.
   */
  void invalidateProjection(String keycloakSubject);
}
