package com.routeshare.identity.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.routeshare.common.security.RouteShareRoles;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.repository.AppUserRepository;
import com.routeshare.identity.service.AccountRoleService;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Derives roles from the local projection and caches them per subject for a short TTL.
 *
 * <p>Suspension is deliberately <em>not</em> expressed by withholding roles. A suspended user must
 * be told why — screen S13 needs the reason, the case reference and the appeal route — and a
 * missing role produces an anonymous "access denied" from method security before any code that
 * knows the reason runs. Suspension is therefore enforced per request in the identity projection
 * and by the driver guard, both of which answer with {@code ACCOUNT_SUSPENDED}.
 */
@Service
public class AccountRoleServiceImpl implements AccountRoleService {
  private static final int MAX_CACHED_ROLE_SETS = 20_000;

  private final AppUserRepository appUsers;
  private final DriverFacade drivers;
  private final Cache<String, Set<String>> roles;

  public AccountRoleServiceImpl(
      AppUserRepository appUsers,
      DriverFacade drivers,
      @Value("${routeshare.identity.role-cache-ttl-seconds:120}") long cacheTtlSeconds) {
    this.appUsers = appUsers;
    this.drivers = drivers;
    this.roles =
        cacheTtlSeconds <= 0
            ? null
            : Caffeine.newBuilder()
                .maximumSize(MAX_CACHED_ROLE_SETS)
                .expireAfterWrite(Duration.ofSeconds(cacheTtlSeconds))
                .build();
  }

  @Override
  @Transactional(readOnly = true)
  public Set<String> effectiveRoles(String keycloakSubject) {
    if (keycloakSubject == null || keycloakSubject.isBlank()) {
      return Set.of();
    }
    if (roles == null) {
      return derive(keycloakSubject);
    }
    return roles.get(keycloakSubject, this::derive);
  }

  @Override
  public void invalidate(String keycloakSubject) {
    if (roles != null && keycloakSubject != null) {
      roles.invalidate(keycloakSubject);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public void invalidateByAppUserId(long appUserId) {
    appUsers.findById(appUserId).map(u -> u.getKeycloakSubject()).ifPresent(this::invalidate);
  }

  private Set<String> derive(String keycloakSubject) {
    var user = appUsers.findByKeycloakSubject(keycloakSubject);
    if (user.isEmpty()) {
      // First request of a brand-new phone-OTP account: the projection is written moments later by
      // the identity facade. Riding is the safe default; driving needs an approved profile anyway.
      return Set.of(RouteShareRoles.PASSENGER);
    }
    long appUserId = user.get().getId();
    Set<String> effective = new LinkedHashSet<>();
    effective.add(RouteShareRoles.PASSENGER);
    if (drivers.findApprovedDriverProfileIdByAppUserId(appUserId).isPresent()
        && !drivers.isDeactivated(appUserId)) {
      effective.add(RouteShareRoles.DRIVER);
    }
    return Set.copyOf(effective);
  }
}
