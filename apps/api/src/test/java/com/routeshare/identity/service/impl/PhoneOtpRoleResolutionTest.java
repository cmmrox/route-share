package com.routeshare.identity.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.RouteShareRoles;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.entity.AppUserEntity;
import com.routeshare.identity.repository.AppUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The slice's core fix: a phone-OTP token carries the account's real roles rather than a hardcoded
 * {@code PASSENGER}, so one account can ride and drive on one token.
 */
class PhoneOtpRoleResolutionTest {
  private static final String SUBJECT = "phone:+94771234567";
  private static final long APP_USER_ID = 42L;

  private final AppUserRepository appUsers = mock(AppUserRepository.class);
  private final DriverFacade drivers = mock(DriverFacade.class);

  private AccountRoleServiceImpl service(long ttlSeconds) {
    return new AccountRoleServiceImpl(appUsers, drivers, ttlSeconds);
  }

  private void account() {
    var entity = mock(AppUserEntity.class);
    when(entity.getId()).thenReturn(APP_USER_ID);
    when(entity.getKeycloakSubject()).thenReturn(SUBJECT);
    when(appUsers.findByKeycloakSubject(SUBJECT)).thenReturn(Optional.of(entity));
    when(appUsers.findById(APP_USER_ID)).thenReturn(Optional.of(entity));
  }

  private void approvedDriver(boolean approved) {
    when(drivers.findApprovedDriverProfileIdByAppUserId(APP_USER_ID))
        .thenReturn(approved ? Optional.of(7L) : Optional.empty());
  }

  @Test
  void aRiderOnlyAccountGetsPassengerOnly() {
    account();
    approvedDriver(false);

    assertThat(service(120).effectiveRoles(SUBJECT)).containsExactly(RouteShareRoles.PASSENGER);
  }

  @Test
  void anApprovedDriverGetsBothRolesOnTheSameToken() {
    account();
    approvedDriver(true);
    when(drivers.isDeactivated(APP_USER_ID)).thenReturn(false);

    assertThat(service(120).effectiveRoles(SUBJECT))
        .containsExactlyInAnyOrder(RouteShareRoles.PASSENGER, RouteShareRoles.DRIVER);
  }

  @Test
  void aDeactivatedDriverLosesTheDriverRoleButKeepsRiding() {
    account();
    approvedDriver(true);
    when(drivers.isDeactivated(APP_USER_ID)).thenReturn(true);

    assertThat(service(120).effectiveRoles(SUBJECT)).containsExactly(RouteShareRoles.PASSENGER);
  }

  @Test
  void aProfileThatIsNotApprovedDoesNotGrantTheDriverRole() {
    account();
    approvedDriver(false);
    when(drivers.findDriverProfileIdByAppUserId(APP_USER_ID)).thenReturn(Optional.of(7L));

    assertThat(service(120).effectiveRoles(SUBJECT)).doesNotContain(RouteShareRoles.DRIVER);
  }

  @Test
  void anUnknownSubjectFallsBackToRidingOnly() {
    when(appUsers.findByKeycloakSubject(SUBJECT)).thenReturn(Optional.empty());

    assertThat(service(120).effectiveRoles(SUBJECT)).containsExactly(RouteShareRoles.PASSENGER);
  }

  @Test
  void aBlankSubjectGetsNothing() {
    assertThat(service(120).effectiveRoles(null)).isEmpty();
    assertThat(service(120).effectiveRoles("  ")).isEmpty();
  }
}
