package com.routeshare.identity.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.RouteShareRoles;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.entity.AppUserEntity;
import com.routeshare.identity.repository.AppUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A revoked role that keeps working until a cache expires is not staleness, it is an open
 * authorization hole. These tests pin the invalidation that closes it.
 */
class RoleCacheInvalidationTest {
  private static final String SUBJECT = "phone:+94771234567";
  private static final long APP_USER_ID = 42L;

  private final AppUserRepository appUsers = mock(AppUserRepository.class);
  private final DriverFacade drivers = mock(DriverFacade.class);
  private final AccountRoleServiceImpl service = new AccountRoleServiceImpl(appUsers, drivers, 300);

  @BeforeEach
  void setUp() {
    var entity = mock(AppUserEntity.class);
    when(entity.getId()).thenReturn(APP_USER_ID);
    when(entity.getKeycloakSubject()).thenReturn(SUBJECT);
    when(appUsers.findByKeycloakSubject(SUBJECT)).thenReturn(Optional.of(entity));
    when(appUsers.findById(APP_USER_ID)).thenReturn(Optional.of(entity));
    when(drivers.findApprovedDriverProfileIdByAppUserId(APP_USER_ID)).thenReturn(Optional.of(7L));
  }

  @Test
  void aRevokedRoleStopsWorkingOnTheNextRequestAfterInvalidation() {
    when(drivers.isDeactivated(APP_USER_ID)).thenReturn(false);
    assertThat(service.effectiveRoles(SUBJECT)).contains(RouteShareRoles.DRIVER);

    when(drivers.isDeactivated(APP_USER_ID)).thenReturn(true);
    service.invalidate(SUBJECT);

    assertThat(service.effectiveRoles(SUBJECT)).doesNotContain(RouteShareRoles.DRIVER);
  }

  @Test
  void invalidationWorksWhenOnlyTheLocalIdIsAtHand() {
    when(drivers.isDeactivated(APP_USER_ID)).thenReturn(false);
    assertThat(service.effectiveRoles(SUBJECT)).contains(RouteShareRoles.DRIVER);

    when(drivers.isDeactivated(APP_USER_ID)).thenReturn(true);
    service.invalidateByAppUserId(APP_USER_ID);

    assertThat(service.effectiveRoles(SUBJECT)).doesNotContain(RouteShareRoles.DRIVER);
  }

  @Test
  void withoutInvalidationTheCachedSetIsServed() {
    // Stated so the fix above is not mistaken for a coincidence: the cache really does hold, which
    // is exactly why every grant, revoke and deactivation has to invalidate it.
    when(drivers.isDeactivated(APP_USER_ID)).thenReturn(false);
    assertThat(service.effectiveRoles(SUBJECT)).contains(RouteShareRoles.DRIVER);

    when(drivers.isDeactivated(APP_USER_ID)).thenReturn(true);

    assertThat(service.effectiveRoles(SUBJECT)).contains(RouteShareRoles.DRIVER);
  }

  @Test
  void aZeroTtlDisablesCachingEntirely() {
    var uncached = new AccountRoleServiceImpl(appUsers, drivers, 0);
    when(drivers.isDeactivated(APP_USER_ID)).thenReturn(false);
    assertThat(uncached.effectiveRoles(SUBJECT)).contains(RouteShareRoles.DRIVER);

    when(drivers.isDeactivated(APP_USER_ID)).thenReturn(true);

    assertThat(uncached.effectiveRoles(SUBJECT)).doesNotContain(RouteShareRoles.DRIVER);
  }
}
