package com.routeshare.identity.service;

import java.util.Set;

/**
 * The authorities an account actually holds, resolved from the local identity projection rather
 * than from whatever the token happened to say.
 *
 * <p>ComiGo has two token issuers. A Keycloak JWT carries realm roles and is converted by {@code
 * KeycloakJwtRoleConverter}. The phone-OTP path mints its own token and used to hardcode {@code
 * ROLE_PASSENGER} into every session — which in a single app meant a phone-OTP user could never
 * drive. Both issuers now end up with the same authorities for the same person, because this
 * service derives them from {@code identity.app_user} and the driver profile.
 *
 * <p>Two rules make the derivation safe to cache:
 *
 * <ul>
 *   <li>A driver profile alone is not driver access — only an <b>approved</b> profile with no open
 *       deactivation grants {@code DRIVER}.
 *   <li>Any change that can take a role away (approval reversal, deactivation, suspension) must
 *       call {@link #invalidate(String)}. A revoked driver still holding a cached role set is a
 *       live authorization hole, not a staleness inconvenience.
 * </ul>
 */
public interface AccountRoleService {
  /** Effective role names (without the {@code ROLE_} prefix) for a Keycloak subject. */
  Set<String> effectiveRoles(String keycloakSubject);

  /** Drops the cached role set for a subject so the next request re-derives it. */
  void invalidate(String keycloakSubject);

  /** Drops the cached role set for an app user, when only the local id is at hand. */
  void invalidateByAppUserId(long appUserId);
}
