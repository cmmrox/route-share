package com.routeshare.identity.facade;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.identity.domain.AppUser;
import java.time.Instant;
import java.util.Optional;
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
   * Grants one managed role to an account and drops its cached authorities.
   *
   * <p>Keycloak is the authority for its own tokens; the phone-OTP path resolves authorities from
   * the local projection instead. Both are updated here, and a Keycloak that is switched off
   * locally does not fail the business operation — the local derivation still holds.
   */
  void grantRealmRole(long appUserId, String role);

  /** Revokes one managed role from an account and drops its cached authorities immediately. */
  void revokeRealmRole(long appUserId, String role);

  /**
   * Drops the cached projection for a subject so status changes (suspend/activate) are enforced on
   * the user's next request instead of after cache expiry.
   */
  void invalidateProjection(String keycloakSubject);

  /**
   * Resolves the local app user for the token <em>without</em> the ACTIVE guard.
   *
   * <p>{@link #upsertFromToken} deliberately refuses a suspended account, which is right for every
   * business endpoint. The app shell is the exception: a suspended user must be told why and shown
   * the appeal route (screen S13), so this variant returns the projection and lets the caller read
   * {@link AppUser#localStatus()}. Never use it to authorise an action.
   */
  AppUser upsertFromTokenAllowingSuspended(CurrentUser currentUser);

  /** Most recent account status change, for the reason and case reference shown on S13. */
  Optional<StatusChange> latestStatusChange(long appUserId);

  /** The mode the app should reopen in, empty until the user has switched at least once. */
  Optional<String> lastActiveMode(long appUserId);

  /**
   * Persists the mode the user is switching into, so the next cold start lands in the same place.
   */
  void setLastActiveMode(long appUserId, String mode);

  /**
   * A person's first name and dialable number, for the counterparty disclosure in plan §6.1.
   *
   * <p>Deliberately narrow: no email, no surname, no account state. The caller has already decided
   * the disclosure is allowed, and this must not become the convenient way to read a user record.
   *
   * <p>A phone-OTP account carries its number as its display name, so a name that is not plainly a
   * name is reported as absent rather than echoed back.
   */
  Optional<Contact> findContact(long appUserId);

  record Contact(long appUserId, String firstName, String phoneNumber) {}

  record StatusChange(String toStatus, String reason, String caseRef, Instant changedAt) {}
}
