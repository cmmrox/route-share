package com.routeshare.identity.service;

import java.util.Set;

/**
 * Propagates RouteShare realm-role changes to the project's Keycloak (the configured {@code
 * routeshare.keycloak-admin} instance). Only roles in the managed RouteShare set are touched, so
 * unrelated realm roles are left intact.
 */
public interface KeycloakRealmRoleService {
  /**
   * Ensures the user (by Keycloak subject/user-id) holds exactly {@code desiredRoles} of the
   * managed set.
   */
  void setRealmRoles(String keycloakSubject, Set<String> desiredRoles);

  /**
   * Adds one managed role, leaving every other role alone.
   *
   * <p>{@link #setRealmRoles} states the whole managed set, which is right for the admin
   * role-editing screen and wrong everywhere else: driver approval knows only that {@code DRIVER}
   * should be added, and using the set form there would silently strip an admin who also drives.
   */
  void grantRealmRole(String keycloakSubject, String role);

  /** Removes one managed role, leaving every other role alone. */
  void revokeRealmRole(String keycloakSubject, String role);
}
