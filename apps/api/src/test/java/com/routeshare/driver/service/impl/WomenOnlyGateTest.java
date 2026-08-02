package com.routeshare.driver.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.routeshare.common.errors.GateCodes;
import com.routeshare.common.errors.GateDeniedException;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.dto.request.DrivingPreferenceRequest;
import com.routeshare.driver.entity.DriverProfileEntity;
import com.routeshare.driver.entity.DrivingPreferenceEntity;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.driver.repository.DrivingPreferenceRepository;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The first of the two women-only gates: who may <em>set</em> it.
 *
 * <p>Without this one, anybody could advertise a women-only car, which is precisely the promise the
 * feature exists to make good on. The second gate — who may book — is {@code
 * EligibilityServiceTest}.
 */
class WomenOnlyGateTest {

  private static final long APP_USER = 7L;
  private static final long PROFILE = 3L;
  private static final Instant NOW = Instant.parse("2026-08-02T09:41:00Z");

  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identity = mock(IdentityFacade.class);
  private final DriverProfileRepository drivers = mock(DriverProfileRepository.class);
  private final DrivingPreferenceRepository preferences = mock(DrivingPreferenceRepository.class);

  private final DrivingPreferenceServiceImpl service =
      new DrivingPreferenceServiceImpl(
          current, identity, drivers, preferences, Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void signedInAsADriver() {
    var token = new CurrentUser("sub", "n@example.test", "+94771234567", "Nimali", Set.of());
    when(current.requireCurrentUser()).thenReturn(token);
    when(identity.upsertFromToken(token))
        .thenReturn(
            new AppUser(
                APP_USER,
                UUID.randomUUID(),
                "sub",
                "n@example.test",
                "+94771234567",
                "Nimali",
                "ACTIVE"));
    when(drivers.findIdByAppUserId(APP_USER)).thenReturn(Optional.of(PROFILE));
    when(preferences.findById(PROFILE))
        .thenReturn(Optional.of(DrivingPreferenceEntity.defaultsFor(PROFILE, NOW)));
    when(preferences.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(call -> call.getArgument(0));
  }

  @Test
  @DisplayName("08-7: a male driver setting women-only is refused")
  void maleDriverCannotSetWomenOnly() {
    givenDriverGender("MALE");

    assertThatThrownBy(() -> service.update(request("WOMEN_ONLY")))
        .isInstanceOf(GateDeniedException.class)
        .extracting(ex -> ((GateDeniedException) ex).code())
        .isEqualTo(GateCodes.WOMEN_ONLY_NOT_AVAILABLE);
  }

  @Test
  @DisplayName("08-8: an unverified driver setting women-only is refused")
  void unverifiedDriverCannotSetWomenOnly() {
    // Gender is null until a reviewer has read the NIC, which is the whole of what "verified"
    // means here.
    givenDriverGender(null);

    assertThatThrownBy(() -> service.update(request("WOMEN_ONLY")))
        .isInstanceOf(GateDeniedException.class);
  }

  @Test
  @DisplayName("08-9: a verified female driver may set women-only")
  void verifiedFemaleDriverMaySetWomenOnly() {
    givenDriverGender("FEMALE");

    var result = service.update(request("WOMEN_ONLY"));

    assertThat(result.genderPolicy()).isEqualTo("WOMEN_ONLY");
    assertThat(result.canSetWomenOnly()).isTrue();
  }

  @Test
  @DisplayName("the gate applies to the toggle only — every other preference is free to change")
  void otherPreferencesAreUngated() {
    givenDriverGender("MALE");

    var result =
        service.update(new DrivingPreferenceRequest("ANYONE", true, false, false, false, false));

    assertThat(result.verifiedRidersOnly()).isTrue();
    assertThat(result.approveEachRequest()).isFalse();
    assertThat(result.canSetWomenOnly()).isFalse();
  }

  @Test
  @DisplayName("the toggle is not offered to a driver who cannot use it")
  void canSetWomenOnlyIsFalseForANonFemaleDriver() {
    givenDriverGender("MALE");

    assertThat(service.mine().canSetWomenOnly()).isFalse();
  }

  private void givenDriverGender(String gender) {
    var profile = new DriverProfileEntity(PROFILE, APP_USER, "Nimali", "APPROVED", gender);
    when(drivers.findById(PROFILE)).thenReturn(Optional.of(profile));
  }

  private static DrivingPreferenceRequest request(String genderPolicy) {
    return new DrivingPreferenceRequest(genderPolicy, false, true, true, true, true);
  }
}
