package com.routeshare.common.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.errors.GateCodes;
import com.routeshare.common.errors.GateDeniedException;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.facade.IdentityFacade;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Suspension outranks every driver gate. A suspended driver under review must see S13 — the reason
 * and the appeal route — and never S08's "we're checking your documents", because only one of those
 * is both true and actionable.
 */
class SuspensionPrecedenceTest {
  private final CurrentUserProvider currentUsers = mock(CurrentUserProvider.class);
  private final IdentityFacade identity = mock(IdentityFacade.class);
  private final DriverFacade drivers = mock(DriverFacade.class);

  private final DriverGuard guard = new DriverGuard(currentUsers, identity, drivers);

  private final CurrentUser token =
      new CurrentUser("subject-1", null, "+94771234567", "Nimali", Set.of("PASSENGER", "DRIVER"));

  @BeforeEach
  void setUp() {
    when(currentUsers.requireCurrentUser()).thenReturn(token);
    // The ACTIVE guard in the identity projection is where suspension is enforced, on every
    // request rather than at token mint.
    when(identity.upsertFromToken(token)).thenThrow(GateDeniedException.accountSuspended());
  }

  @Test
  void aSuspendedAccountIsRefusedWithTheSuspensionCodeNotADriverGate() {
    assertThatThrownBy(() -> guard.canDrive(null))
        .isInstanceOf(GateDeniedException.class)
        .extracting(ex -> ((GateDeniedException) ex).code())
        .isEqualTo(GateCodes.ACCOUNT_SUSPENDED);
  }

  @Test
  void driverGatesAreNotEvenComputedForASuspendedAccount() {
    assertThatThrownBy(() -> guard.canDrive(null)).isInstanceOf(GateDeniedException.class);

    verify(drivers, never()).gatesFor(anyLong());
  }

  @Test
  void publishingIsRefusedWithTheSameCode() {
    assertThatThrownBy(() -> guard.canPublish(null))
        .extracting(ex -> ((GateDeniedException) ex).code())
        .isEqualTo(GateCodes.ACCOUNT_SUSPENDED);
  }

  @Test
  void driverAccountSurfacesAreRefusedToo() {
    // Deactivation leaves earnings and support open; suspension does not.
    assertThatThrownBy(() -> guard.canManageDriverAccount(null))
        .extracting(ex -> ((GateDeniedException) ex).code())
        .isEqualTo(GateCodes.ACCOUNT_SUSPENDED);
  }

  @Test
  void theSuspensionMessageCarriesAnAppealRoute() {
    assertThatThrownBy(() -> guard.canDrive(null))
        .extracting(ex -> ((GateDeniedException) ex).actionPath())
        .isEqualTo("/support");
  }
}
