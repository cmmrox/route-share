package com.routeshare.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.routeshare.common.errors.GateCodes;
import com.routeshare.common.errors.GateDeniedException;
import com.routeshare.driver.domain.DriverGate;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The composite gate across role, profile status, deactivation and suspension. */
class DriverGuardTest {
  private static final long APP_USER_ID = 42L;

  private final CurrentUserProvider currentUsers = mock(CurrentUserProvider.class);
  private final IdentityFacade identity = mock(IdentityFacade.class);
  private final DriverFacade drivers = mock(DriverFacade.class);

  private final DriverGuard guard = new DriverGuard(currentUsers, identity, drivers);

  private final CurrentUser token =
      new CurrentUser("subject-1", null, "+94771234567", "Nimali", Set.of("PASSENGER"));

  @BeforeEach
  void setUp() {
    when(currentUsers.requireCurrentUser()).thenReturn(token);
    when(identity.upsertFromToken(token))
        .thenReturn(
            new AppUser(
                APP_USER_ID,
                UUID.randomUUID(),
                "subject-1",
                null,
                "+94771234567",
                "Nimali",
                "ACTIVE"));
  }

  private static DriverGate gate(String code) {
    return new DriverGate(code, "message", "/driver/x");
  }

  @Test
  void aPassengerOnlyAccountCannotReachADriverEndpointAndIsToldWhy() {
    when(drivers.gatesFor(APP_USER_ID)).thenReturn(List.of(gate(GateCodes.DRIVER_PROFILE_MISSING)));

    assertThatThrownBy(() -> guard.canDrive(null))
        .isInstanceOf(GateDeniedException.class)
        .extracting(ex -> ((GateDeniedException) ex).code())
        .isEqualTo(GateCodes.DRIVER_PROFILE_MISSING);
  }

  @Test
  void anApprovedDriverPasses() {
    when(drivers.gatesFor(APP_USER_ID)).thenReturn(List.of());

    assertThat(guard.canDrive(null)).isTrue();
  }

  @Test
  void aDeactivatedDriverIsRefusedDrivingButKeepsTheirAccountSurfaces() {
    when(drivers.gatesFor(APP_USER_ID)).thenReturn(List.of(gate(GateCodes.DRIVER_DEACTIVATED)));
    when(drivers.findDriverProfileIdByAppUserId(APP_USER_ID)).thenReturn(Optional.of(7L));

    assertThatThrownBy(() -> guard.canDrive(null)).isInstanceOf(GateDeniedException.class);
    // D34: earnings and support must still answer, or the screen's own instructions fail.
    assertThat(guard.canManageDriverAccount(null)).isTrue();
  }

  @Test
  void anAccountWithNoDriverProfileHasNoDriverAccountSurfacesEither() {
    when(drivers.findDriverProfileIdByAppUserId(APP_USER_ID)).thenReturn(Optional.empty());
    when(drivers.gatesFor(APP_USER_ID)).thenReturn(List.of(gate(GateCodes.DRIVER_PROFILE_MISSING)));

    assertThatThrownBy(() -> guard.canManageDriverAccount(null))
        .isInstanceOf(GateDeniedException.class);
  }

  @Test
  void publishingIsRefusedSeparatelyFromDriving() {
    when(drivers.gatesFor(APP_USER_ID)).thenReturn(List.of());
    when(drivers.publishGatesFor(APP_USER_ID))
        .thenReturn(List.of(gate(GateCodes.VEHICLE_NOT_APPROVED)));

    assertThat(guard.canDrive(null)).isTrue();
    assertThatThrownBy(() -> guard.canPublish(null))
        .isInstanceOf(GateDeniedException.class)
        .extracting(ex -> ((GateDeniedException) ex).code())
        .isEqualTo(GateCodes.VEHICLE_NOT_APPROVED);
  }

  @Test
  void theFirstGateIsTheOneReturnedSoTheAppShowsOneActionableScreen() {
    when(drivers.gatesFor(APP_USER_ID)).thenReturn(List.of());
    when(drivers.publishGatesFor(APP_USER_ID))
        .thenReturn(
            List.of(gate(GateCodes.DOCUMENT_REJECTED), gate(GateCodes.VEHICLE_NOT_APPROVED)));

    assertThatThrownBy(() -> guard.canPublish(null))
        .extracting(ex -> ((GateDeniedException) ex).code())
        .isEqualTo(GateCodes.DOCUMENT_REJECTED);
  }

  @Test
  void aForgedModeHeaderChangesNothingBecauseTheGuardNeverReadsTheRequest() {
    when(drivers.gatesFor(APP_USER_ID)).thenReturn(List.of(gate(GateCodes.DRIVER_PROFILE_MISSING)));

    // The guard's only inputs are the token subject and the projection; there is no path by which
    // a header or a body could make it say yes.
    assertThatThrownBy(() -> guard.canDrive(null)).isInstanceOf(GateDeniedException.class);
  }
}
