package com.routeshare.passenger.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.passenger.repository.PassengerProfileRepository;
import com.routeshare.passenger.service.PhotoVisibilityService.ViewContext;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PUBLIC/MATCHED/HIDDEN × viewer × booking state, including the driver asymmetry.
 *
 * <p>The important assertions are the empty ones. A {@code HIDDEN} photo URL must not appear in a
 * payload at all — not emitted and then dropped by the client — because a URL in a response is a
 * URL in a log, a cache and a proxy, and none of those was part of the choice she made.
 */
class PhotoVisibilityMatrixTest {

  private static final long RIDER = 42L;
  private static final long DRIVER = 99L;
  private static final String PHOTO = "https://storage.local/photos/42.jpg";

  private final PassengerProfileRepository profiles = mock(PassengerProfileRepository.class);

  private final PhotoVisibilityServiceImpl service =
      new PhotoVisibilityServiceImpl(
          mock(CurrentUserProvider.class), mock(IdentityFacade.class), profiles);

  // ── PUBLIC ───────────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("PUBLIC is visible in search")
  void publicIsVisibleInSearch() {
    givenRider("PUBLIC");
    assertThat(service.resolve(DRIVER, RIDER, ViewContext.SEARCH)).contains(PHOTO);
  }

  @Test
  @DisplayName("PUBLIC is visible on an unanswered request")
  void publicIsVisibleOnAPendingRequest() {
    givenRider("PUBLIC");
    assertThat(service.resolve(DRIVER, RIDER, ViewContext.PENDING_REQUEST)).contains(PHOTO);
  }

  // ── MATCHED ──────────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("08-17: MATCHED gives no URL before the booking is confirmed")
  void matchedIsHiddenBeforeConfirmation() {
    givenRider("MATCHED");
    assertThat(service.resolve(DRIVER, RIDER, ViewContext.PENDING_REQUEST)).isEmpty();
  }

  @Test
  @DisplayName("MATCHED gives no URL in search")
  void matchedIsHiddenInSearch() {
    givenRider("MATCHED");
    assertThat(service.resolve(DRIVER, RIDER, ViewContext.SEARCH)).isEmpty();
  }

  @Test
  @DisplayName("08-18: MATCHED opens to the confirmed driver")
  void matchedOpensOnConfirmation() {
    givenRider("MATCHED");
    assertThat(service.resolve(DRIVER, RIDER, ViewContext.CONFIRMED_BOOKING)).contains(PHOTO);
  }

  // ── HIDDEN ───────────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("08-16: HIDDEN gives no URL even to the confirmed driver")
  void hiddenIsHiddenFromTheConfirmedDriver() {
    givenRider("HIDDEN");
    assertThat(service.resolve(DRIVER, RIDER, ViewContext.CONFIRMED_BOOKING)).isEmpty();
  }

  @Test
  @DisplayName("HIDDEN gives no URL in search or on a request")
  void hiddenIsHiddenEverywhere() {
    givenRider("HIDDEN");
    assertThat(service.resolve(DRIVER, RIDER, ViewContext.SEARCH)).isEmpty();
    assertThat(service.resolve(DRIVER, RIDER, ViewContext.PENDING_REQUEST)).isEmpty();
  }

  @Test
  @DisplayName("a rider always sees her own photo, whatever she has hidden it from")
  void ownPhotoIsAlwaysVisible() {
    givenRider("HIDDEN");
    assertThat(service.resolve(RIDER, RIDER, ViewContext.SELF)).contains(PHOTO);
  }

  // ── the asymmetry ────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("08-19: the driver's photo always reaches a confirmed rider, even set to HIDDEN")
  void driverPhotoAlwaysReachesAConfirmedRider() {
    // She is getting into his car and has to know it is him (D35). His own preference does not
    // enter into it.
    givenSubject(DRIVER, "HIDDEN");
    assertThat(service.resolve(RIDER, DRIVER, ViewContext.CONFIRMED_BOOKING_DRIVER))
        .contains(PHOTO);
  }

  @Test
  @DisplayName("08-20: the driver's photo is never in a search result")
  void driverPhotoIsNeverInSearch() {
    givenSubject(DRIVER, "MATCHED");
    assertThat(service.resolve(RIDER, DRIVER, ViewContext.SEARCH)).isEmpty();
  }

  // ── nothing on file ──────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("a rider with no photo yields nothing, whatever her setting")
  void noPhotoYieldsNothing() {
    var row = mock(PassengerProfileRepository.RiderProfileRow.class);
    when(row.getPhotoUrl()).thenReturn(null);
    when(profiles.findRiderProfile(RIDER)).thenReturn(Optional.of(row));

    assertThat(service.resolve(DRIVER, RIDER, ViewContext.CONFIRMED_BOOKING)).isEmpty();
  }

  private void givenRider(String visibility) {
    givenSubject(RIDER, visibility);
  }

  private void givenSubject(long appUserId, String visibility) {
    var row = mock(PassengerProfileRepository.RiderProfileRow.class);
    when(row.getPhotoUrl()).thenReturn(PHOTO);
    when(row.getPhotoVisibility()).thenReturn(visibility);
    when(profiles.findRiderProfile(appUserId)).thenReturn(Optional.of(row));
  }
}
