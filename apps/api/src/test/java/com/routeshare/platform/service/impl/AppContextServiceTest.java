package com.routeshare.platform.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.dto.response.PassengerBookingDetailResponse;
import com.routeshare.booking.service.BookingService;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.service.NotificationService;
import com.routeshare.passenger.facade.PassengerFacade;
import com.routeshare.platform.dto.response.AppContextResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppContextServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-01T09:41:00Z");

  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identity = mock(IdentityFacade.class);
  private final PassengerFacade passengers = mock(PassengerFacade.class);
  private final DriverFacade drivers = mock(DriverFacade.class);
  private final BookingService bookings = mock(BookingService.class);
  private final NotificationService notifications = mock(NotificationService.class);

  private final AppContextServiceImpl service =
      new AppContextServiceImpl(
          current,
          identity,
          passengers,
          drivers,
          bookings,
          notifications,
          Clock.fixed(NOW, ZoneOffset.UTC));

  private CurrentUser token(Set<String> roles) {
    return new CurrentUser(
        "subject-1", "nimali@example.test", "+94771234567", "Nimali Perera", roles);
  }

  private AppUser appUser(String localStatus) {
    return new AppUser(
        42L,
        UUID.randomUUID(),
        "subject-1",
        "nimali@example.test",
        "+94771234567",
        "Nimali Perera",
        localStatus);
  }

  private void given(
      Set<String> roles, boolean hasPassengerProfile, String driverStatus, String localStatus) {
    CurrentUser t = token(roles);
    when(current.requireCurrentUser()).thenReturn(t);
    when(identity.upsertFromTokenAllowingSuspended(t)).thenReturn(appUser(localStatus));
    when(passengers.existsPassengerProfileByAppUserId(42L)).thenReturn(hasPassengerProfile);
    when(drivers.findDriverStatusByAppUserId(42L))
        .thenReturn(driverStatus == null ? Optional.empty() : Optional.of(driverStatus));
    when(bookings.getCurrentPassengerTrip()).thenReturn(Optional.empty());
    when(notifications.unreadCount()).thenReturn(0L);
  }

  @Test
  void riderWithNoDriverProfileGetsPassengerModeAndTheBecomeADriverGate() {
    given(Set.of("PASSENGER"), true, null, "ACTIVE");

    AppContextResponse ctx = service.current();

    assertThat(ctx.availableModes()).containsExactly("PASSENGER");
    assertThat(ctx.driver().status()).isEqualTo("NONE");
    assertThat(ctx.driver().gates())
        .extracting(AppContextResponse.Gate::code)
        .containsExactly("DRIVER_PROFILE_MISSING");
    assertThat(ctx.driver().canPublish()).isFalse();
    assertThat(ctx.account().status()).isEqualTo("ACTIVE");
  }

  @Test
  void approvedDriverWithTheRoleGetsBothModes() {
    given(Set.of("PASSENGER", "DRIVER"), true, "APPROVED", "ACTIVE");

    AppContextResponse ctx = service.current();

    assertThat(ctx.availableModes()).containsExactly("PASSENGER", "DRIVER");
    assertThat(ctx.driver().gates()).isEmpty();
    assertThat(ctx.driver().canPublish()).isTrue();
  }

  @Test
  void approvedProfileWithoutTheRealmRoleDoesNotUnlockDriverMode() {
    given(Set.of("PASSENGER"), true, "APPROVED", "ACTIVE");

    // Slice 01 grants the DRIVER realm role on approval. Until the token carries it, the mode chip
    // must stay out of reach — a profile row alone is not authorisation.
    assertThat(service.current().availableModes()).containsExactly("PASSENGER");
  }

  @Test
  void pendingReviewIsReportedAsAGateNotAnError() {
    given(Set.of("PASSENGER"), true, "PENDING_REVIEW", "ACTIVE");

    AppContextResponse ctx = service.current();

    assertThat(ctx.driver().gates())
        .extracting(AppContextResponse.Gate::code)
        .containsExactly("DRIVER_REVIEW_PENDING");
    assertThat(ctx.driver().gates().get(0).actionPath()).isNotBlank();
  }

  @Test
  void rejectedApplicationIsReportedAsAGate() {
    given(Set.of("PASSENGER"), true, "REJECTED", "ACTIVE");

    assertThat(service.current().driver().gates())
        .extracting(AppContextResponse.Gate::code)
        .containsExactly("DRIVER_APPLICATION_REJECTED");
  }

  @Test
  void deactivatedDriverIsGatedButKeepsPassengerMode() {
    given(Set.of("PASSENGER", "DRIVER"), true, "SUSPENDED", "ACTIVE");

    AppContextResponse ctx = service.current();

    assertThat(ctx.driver().gates())
        .extracting(AppContextResponse.Gate::code)
        .containsExactly("DRIVER_DEACTIVATED");
    assertThat(ctx.availableModes()).contains("PASSENGER");
    assertThat(ctx.driver().canPublish()).isFalse();
  }

  @Test
  void suspendedAccountIsAnsweredWithTheReasonRatherThanRefused() {
    given(Set.of("PASSENGER"), true, "APPROVED", "SUSPENDED");
    when(identity.latestStatusChange(42L))
        .thenReturn(
            Optional.of(
                new IdentityFacade.StatusChange(
                    "SUSPENDED", "Two reports of a driver not matching the licence photo", NOW)));

    AppContextResponse ctx = service.current();

    assertThat(ctx.account().status()).isEqualTo("SUSPENDED");
    assertThat(ctx.account().reason()).contains("licence photo");
    assertThat(ctx.account().caseRef()).isNotBlank();
    assertThat(ctx.account().suspendedAt()).isEqualTo(NOW);
  }

  @Test
  void suspensionOutranksEveryDriverGate() {
    // S13 replaces S08/S09; it must not stack with them.
    given(Set.of("PASSENGER", "DRIVER"), true, "PENDING_REVIEW", "SUSPENDED");
    when(identity.latestStatusChange(anyLong())).thenReturn(Optional.empty());

    AppContextResponse ctx = service.current();

    assertThat(ctx.driver().gates())
        .extracting(AppContextResponse.Gate::code)
        .containsExactly("ACCOUNT_SUSPENDED");
    assertThat(ctx.activeTrip()).isNull();
    assertThat(ctx.badges().inbox()).isZero();
  }

  @Test
  void suspendedCallerNeverTriggersTheActiveGuard() {
    given(Set.of("PASSENGER"), true, null, "SUSPENDED");
    when(identity.latestStatusChange(anyLong())).thenReturn(Optional.empty());

    service.current();

    // upsertFromToken throws AccessDeniedException on a suspended account; the shell must not use
    // it.
    verify(identity, never()).upsertFromToken(org.mockito.ArgumentMatchers.any());
    verify(identity).upsertFromTokenAllowingSuspended(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void anActiveRideIsExposedForTheResumeBar() {
    given(Set.of("PASSENGER"), true, null, "ACTIVE");
    when(bookings.getCurrentPassengerTrip())
        .thenReturn(
            Optional.of(
                new PassengerBookingDetailResponse(
                    8042L,
                    1L,
                    2L,
                    7L,
                    "Rajagiriya junction",
                    "Nugegoda",
                    NOW,
                    1,
                    "CONFIRMED",
                    "STARTED",
                    "BOARDED",
                    new BigDecimal("267.00"),
                    "AUTHORIZED",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    NOW)));

    AppContextResponse ctx = service.current();

    assertThat(ctx.activeTrip()).isNotNull();
    assertThat(ctx.activeTrip().kind()).isEqualTo("RIDING");
    assertThat(ctx.activeTrip().bookingId()).isEqualTo(8042L);
    assertThat(ctx.activeTrip().label()).isEqualTo("Rajagiriya junction → Nugegoda");
  }

  @Test
  void aFailingContributorDegradesTheShellRatherThanBreakingIt() {
    given(Set.of("PASSENGER"), true, null, "ACTIVE");
    when(bookings.getCurrentPassengerTrip())
        .thenThrow(new IllegalStateException("trip module down"));
    when(notifications.unreadCount()).thenThrow(new IllegalStateException("notifications down"));

    AppContextResponse ctx = service.current();

    assertThat(ctx.activeTrip()).isNull();
    assertThat(ctx.badges().inbox()).isZero();
    assertThat(ctx.availableModes()).isNotEmpty();
  }

  @Test
  void fieldsOwnedByLaterSlicesReturnZeroValuesNotNulls() {
    given(Set.of("PASSENGER"), true, null, "ACTIVE");

    AppContextResponse ctx = service.current();

    assertThat(ctx.money()).isNotNull();
    assertThat(ctx.money().currency()).isEqualTo("LKR");
    assertThat(ctx.money().outstandingDues()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(ctx.money().rewardsBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(ctx.passenger().verificationLevel()).isEqualTo("NONE");
    assertThat(ctx.badges()).isNotNull();
    assertThat(ctx.settings()).isNotNull();
    assertThat(ctx.serverTime()).isEqualTo(NOW);
  }

  @Test
  void adminRolesExposeTheAdminMode() {
    given(Set.of("PASSENGER", "OPS_ADMIN"), true, null, "ACTIVE");

    assertThat(service.current().availableModes()).contains("ADMIN");
  }

  @Test
  void unreadNotificationsDriveTheInboxBadgeOnly() {
    given(Set.of("PASSENGER"), true, null, "ACTIVE");
    when(notifications.unreadCount()).thenReturn(3L);

    AppContextResponse.Badges badges = service.current().badges();

    assertThat(badges.inbox()).isEqualTo(3);
    // Board S14: home and account are dots, trips is a count, the action tab is never badged.
    assertThat(badges.home()).isFalse();
    assertThat(badges.account()).isFalse();
    assertThat(badges.trips()).isZero();
  }

  @Test
  void contextIsAlwaysResolvedFromTheTokenNeverFromAParameter() throws Exception {
    given(Set.of("PASSENGER"), true, null, "ACTIVE");

    assertThat(service.current().subject()).isEqualTo("subject-1");
    verify(current).requireCurrentUser();
    // The only public entry point takes no arguments, so a caller cannot ask for someone else.
    assertThat(
            com.routeshare.platform.service.AppContextService.class
                .getMethod("current")
                .getParameterCount())
        .isZero();
  }
}
