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
}
