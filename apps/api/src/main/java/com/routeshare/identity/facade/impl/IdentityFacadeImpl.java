package com.routeshare.identity.facade.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.entity.AppUserEntity;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.identity.repository.AppUserRepository;
import com.routeshare.identity.repository.AppUserStatusHistoryRepository;
import com.routeshare.identity.service.AccountRoleService;
import com.routeshare.identity.service.KeycloakRealmRoleService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-module identity entry point. Nearly every authenticated request resolves the local app
 * user, so the token projection is cached per subject for a short TTL — turning a database
 * write-per-request into an in-memory lookup. A projection is only cached after the ACTIVE check
 * passes, changed token claims bypass the cache, and admin suspend/activate invalidates the entry
 * immediately.
 */
@Component
public class IdentityFacadeImpl implements IdentityFacade {
  private static final int MAX_CACHED_PROJECTIONS = 20_000;

  private static final Logger log = LoggerFactory.getLogger(IdentityFacadeImpl.class);

  private final AppUserRepository appUsers;
  private final AppUserStatusHistoryRepository statusHistory;
  private final KeycloakRealmRoleService keycloakRealmRoleService;
  private final AccountRoleService accountRoles;
  private final MeterRegistry meters;
  private final Cache<String, CachedProjection> projections;

  public IdentityFacadeImpl(
      AppUserRepository appUsers,
      AppUserStatusHistoryRepository statusHistory,
      KeycloakRealmRoleService keycloakRealmRoleService,
      AccountRoleService accountRoles,
      MeterRegistry meters,
      @Value("${routeshare.identity.projection-cache-ttl-seconds:300}") long cacheTtlSeconds) {
    this.appUsers = appUsers;
    this.statusHistory = statusHistory;
    this.keycloakRealmRoleService = keycloakRealmRoleService;
    this.accountRoles = accountRoles;
    this.meters = meters;
    this.projections =
        cacheTtlSeconds <= 0
            ? null
            : Caffeine.newBuilder()
                .maximumSize(MAX_CACHED_PROJECTIONS)
                .expireAfterWrite(Duration.ofSeconds(cacheTtlSeconds))
                .build();
  }

  @Override
  public AppUser upsertFromToken(CurrentUser currentUser) {
    if (projections == null) {
      return appUsers.upsertFromToken(currentUser);
    }
    CachedProjection cached = projections.getIfPresent(currentUser.subject());
    if (cached != null && cached.matches(currentUser)) {
      return cached.appUser();
    }
    AppUser appUser = appUsers.upsertFromToken(currentUser);
    projections.put(currentUser.subject(), CachedProjection.of(currentUser, appUser));
    return appUser;
  }

  @Override
  public void setRealmRoles(String keycloakSubject, Set<String> roles) {
    keycloakRealmRoleService.setRealmRoles(keycloakSubject, roles);
    accountRoles.invalidate(keycloakSubject);
  }

  @Override
  public void grantRealmRole(long appUserId, String role) {
    changeRealmRole(appUserId, role, true);
  }

  @Override
  public void revokeRealmRole(long appUserId, String role) {
    changeRealmRole(appUserId, role, false);
  }

  private void changeRealmRole(long appUserId, String role, boolean grant) {
    var user = appUsers.findById(appUserId);
    if (user.isEmpty()) {
      return;
    }
    String subject = user.get().getKeycloakSubject();
    try {
      if (grant) {
        keycloakRealmRoleService.grantRealmRole(subject, role);
      } else {
        keycloakRealmRoleService.revokeRealmRole(subject, role);
      }
    } catch (RuntimeException ex) {
      // A local stack often runs with Keycloak admin switched off. Authorities for phone-OTP
      // tokens come from the local projection, so the business decision still holds; losing the
      // realm mapping is a warning, not a failed approval.
      log.warn(
          "realm role {} for app user {} could not be propagated to Keycloak",
          grant ? "grant" : "revoke",
          appUserId,
          ex);
    }
    meters
        .counter("routeshare_role_grant_total", "role", role, "action", grant ? "GRANT" : "REVOKE")
        .increment();
    // Authorities are cached; a revoke that only takes effect at TTL expiry is an open hole.
    accountRoles.invalidate(subject);
    invalidateProjection(subject);
  }

  @Override
  public AppUser upsertFromTokenAllowingSuspended(CurrentUser currentUser) {
    appUsers.upsertTokenUser(
        currentUser.subject(), currentUser.email(), currentUser.phone(), currentUser.displayName());
    return appUsers.findBySubject(currentUser.subject()).orElseThrow();
  }

  @Override
  public Optional<StatusChange> latestStatusChange(long appUserId) {
    return statusHistory.findByAppUserIdOrderByIdDesc(appUserId).stream()
        .findFirst()
        .map(
            h ->
                new StatusChange(h.getToStatus(), h.getReason(), h.getCaseRef(), h.getCreatedAt()));
  }

  @Override
  public Optional<String> lastActiveMode(long appUserId) {
    return appUsers.findById(appUserId).map(AppUserEntity::getLastActiveMode);
  }

  @Override
  @Transactional
  public void setLastActiveMode(long appUserId, String mode) {
    appUsers
        .findById(appUserId)
        .ifPresent(
            user -> {
              user.setLastActiveMode(mode);
              appUsers.save(user);
              // The projection carries no mode, but a stale entry would shadow this write on the
              // next read of the same row.
              invalidateProjection(user.getKeycloakSubject());
            });
  }

  @Override
  public void invalidateProjection(String keycloakSubject) {
    if (projections != null && keycloakSubject != null) {
      projections.invalidate(keycloakSubject);
    }
  }

  record CachedProjection(String email, String phone, String displayName, AppUser appUser) {
    static CachedProjection of(CurrentUser currentUser, AppUser appUser) {
      return new CachedProjection(
          currentUser.email(), currentUser.phone(), currentUser.displayName(), appUser);
    }

    boolean matches(CurrentUser currentUser) {
      return Objects.equals(email, currentUser.email())
          && Objects.equals(phone, currentUser.phone())
          && Objects.equals(displayName, currentUser.displayName());
    }
  }
}
