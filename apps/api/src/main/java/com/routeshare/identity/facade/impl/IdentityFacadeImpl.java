package com.routeshare.identity.facade.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.identity.repository.AppUserRepository;
import com.routeshare.identity.service.KeycloakRealmRoleService;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

  private final AppUserRepository appUsers;
  private final KeycloakRealmRoleService keycloakRealmRoleService;
  private final Cache<String, CachedProjection> projections;

  public IdentityFacadeImpl(
      AppUserRepository appUsers,
      KeycloakRealmRoleService keycloakRealmRoleService,
      @Value("${routeshare.identity.projection-cache-ttl-seconds:300}") long cacheTtlSeconds) {
    this.appUsers = appUsers;
    this.keycloakRealmRoleService = keycloakRealmRoleService;
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
