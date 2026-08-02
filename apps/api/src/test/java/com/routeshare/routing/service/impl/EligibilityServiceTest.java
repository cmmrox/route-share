package com.routeshare.routing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.errors.GateCodes;
import com.routeshare.common.errors.GateDeniedException;
import com.routeshare.passenger.facade.PassengerFacade;
import com.routeshare.passenger.facade.PassengerFacade.RiderEligibilityProfile;
import com.routeshare.routing.entity.EligibilityDenialEntity;
import com.routeshare.routing.entity.RouteOccurrenceEntity;
import com.routeshare.routing.repository.EligibilityDenialRepository;
import com.routeshare.routing.repository.RouteOccurrenceRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The full rider × trip matrix.
 *
 * <p>Every cell here is somebody being let into or kept out of a stranger's car, so the table is
 * exhaustive rather than representative: an ordinary trip has to admit everyone, and each
 * restricted trip has to refuse for exactly one stated reason.
 */
class EligibilityServiceTest {

  private static final long OCCURRENCE = 77L;
  private static final long RIDER = 42L;

  private final RouteOccurrenceRepository occurrences = mock(RouteOccurrenceRepository.class);
  private final PassengerFacade passengers = mock(PassengerFacade.class);
  private final EligibilityDenialRepository denials = mock(EligibilityDenialRepository.class);

  private final EligibilityServiceImpl service =
      new EligibilityServiceImpl(occurrences, passengers, denials, new SimpleMeterRegistry());

  // ── an ordinary trip admits everyone ─────────────────────────────────────────────────────────

  @Test
  @DisplayName("08-6: an unverified rider may book an ordinary trip")
  void ordinaryTripAdmitsAnUnverifiedRider() {
    givenTrip("ANYONE", false);
    givenRider("NONE", "UNSPECIFIED");

    assertThat(service.canBook(RIDER, OCCURRENCE).allowed()).isTrue();
  }

  @Test
  @DisplayName("a rejected rider may still book an ordinary trip — verification is never a gate")
  void ordinaryTripAdmitsARejectedRider() {
    givenTrip("ANYONE", false);
    givenRider("REJECTED", "UNSPECIFIED");

    assertThat(service.canBook(RIDER, OCCURRENCE).allowed()).isTrue();
  }

  @Test
  @DisplayName("an ordinary trip never reads the rider's profile at all")
  void ordinaryTripDoesNotEvenLookAtTheRider() {
    givenTrip("ANYONE", false);

    assertThat(service.canBook(RIDER, OCCURRENCE).allowed()).isTrue();
    verify(passengers, never()).riderEligibilityProfile(anyLong());
  }

  // ── verified-only ────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("08-2: an unverified rider is refused a verified-only trip, with the reason")
  void verifiedOnlyRefusesAnUnverifiedRider() {
    givenTrip("ANYONE", true);
    givenRider("NONE", "UNSPECIFIED");

    var decision = service.canBook(RIDER, OCCURRENCE);

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reason()).isEqualTo(GateCodes.NOT_ELIGIBLE_VERIFIED_ONLY);
  }

  @Test
  @DisplayName("a rider still under review is not yet verified")
  void verifiedOnlyRefusesAPendingRider() {
    givenTrip("ANYONE", true);
    givenRider("PENDING", "FEMALE");

    assertThat(service.canBook(RIDER, OCCURRENCE).reason())
        .isEqualTo(GateCodes.NOT_ELIGIBLE_VERIFIED_ONLY);
  }

  @Test
  @DisplayName("a verified rider of any gender may book a verified-only trip")
  void verifiedOnlyAdmitsAVerifiedRider() {
    givenTrip("ANYONE", true);
    givenRider("VERIFIED", "MALE");

    assertThat(service.canBook(RIDER, OCCURRENCE).allowed()).isTrue();
  }

  // ── women-only ───────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("08-4: a male rider is refused a women-only trip")
  void womenOnlyRefusesAMaleRider() {
    givenTrip("WOMEN_ONLY", false);
    givenRider("VERIFIED", "MALE");

    assertThat(service.canBook(RIDER, OCCURRENCE).reason())
        .isEqualTo(GateCodes.NOT_ELIGIBLE_WOMEN_ONLY);
  }

  @Test
  @DisplayName("an unverified rider is refused a women-only trip — self-declaration is not enough")
  void womenOnlyRefusesAnUnverifiedRider() {
    givenTrip("WOMEN_ONLY", false);
    givenRider("NONE", "FEMALE");

    assertThat(service.canBook(RIDER, OCCURRENCE).reason())
        .isEqualTo(GateCodes.NOT_ELIGIBLE_WOMEN_ONLY);
  }

  @Test
  @DisplayName("08-5: a verified female rider may book a women-only trip")
  void womenOnlyAdmitsAVerifiedFemaleRider() {
    givenTrip("WOMEN_ONLY", false);
    givenRider("VERIFIED", "FEMALE");

    assertThat(service.canBook(RIDER, OCCURRENCE).allowed()).isTrue();
  }

  @Test
  @DisplayName("both rules at once: women-only is the reason given, not verified-only")
  void womenOnlyOutranksVerifiedOnly() {
    givenTrip("WOMEN_ONLY", true);
    givenRider("NONE", "UNSPECIFIED");

    // Naming the weaker reason would send her away to verify and then refuse her a second time for
    // something she cannot change.
    assertThat(service.canBook(RIDER, OCCURRENCE).reason())
        .isEqualTo(GateCodes.NOT_ELIGIBLE_WOMEN_ONLY);
  }

  @Test
  @DisplayName("both rules at once: a verified female rider satisfies both")
  void bothRulesAdmitAVerifiedFemaleRider() {
    givenTrip("WOMEN_ONLY", true);
    givenRider("VERIFIED", "FEMALE");

    assertThat(service.canBook(RIDER, OCCURRENCE).allowed()).isTrue();
  }

  // ── the guard ────────────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("the booking guard throws the typed reason and records the denial")
  void requireEligibleThrowsAndRecords() {
    givenTrip("ANYONE", true);
    givenRider("NONE", "UNSPECIFIED");

    assertThatThrownBy(() -> service.requireEligible(RIDER, OCCURRENCE))
        .isInstanceOf(GateDeniedException.class)
        .extracting(ex -> ((GateDeniedException) ex).code())
        .isEqualTo(GateCodes.NOT_ELIGIBLE_VERIFIED_ONLY);

    verify(denials).save(org.mockito.ArgumentMatchers.any(EligibilityDenialEntity.class));
  }

  @Test
  @DisplayName("an eligible booking records nothing")
  void requireEligibleRecordsNothingWhenAllowed() {
    givenTrip("ANYONE", false);

    service.requireEligible(RIDER, OCCURRENCE);

    verify(denials, never()).save(org.mockito.ArgumentMatchers.any(EligibilityDenialEntity.class));
  }

  @Test
  @DisplayName("a trip that does not exist is not refused here — the seat reservation refuses it")
  void unknownTripIsAllowedThrough() {
    when(occurrences.findById(OCCURRENCE)).thenReturn(Optional.empty());

    assertThat(service.canBook(RIDER, OCCURRENCE).allowed()).isTrue();
  }

  // ── helpers ──────────────────────────────────────────────────────────────────────────────────

  private void givenTrip(String genderPolicy, boolean verifiedRidersOnly) {
    var occurrence = mock(RouteOccurrenceEntity.class);
    when(occurrence.getGenderPolicy()).thenReturn(genderPolicy);
    when(occurrence.isVerifiedRidersOnly()).thenReturn(verifiedRidersOnly);
    when(occurrences.findById(OCCURRENCE)).thenReturn(Optional.of(occurrence));
  }

  private void givenRider(String level, String gender) {
    when(passengers.riderEligibilityProfile(RIDER))
        .thenReturn(new RiderEligibilityProfile(level, gender));
  }
}
