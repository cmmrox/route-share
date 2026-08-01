package com.routeshare.vehicle.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.errors.GateCodes;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.vehicle.domain.RateBandCodes;
import com.routeshare.vehicle.dto.request.RateBandAssessmentCommand;
import com.routeshare.vehicle.dto.request.RateBandReviewDecisionCommand;
import com.routeshare.vehicle.dto.request.RateBandReviewRequestCommand;
import com.routeshare.vehicle.entity.RateBandReviewRequestEntity;
import com.routeshare.vehicle.entity.VehicleClassEntity;
import com.routeshare.vehicle.entity.VehicleEntity;
import com.routeshare.vehicle.entity.VehicleRateBandEntity;
import com.routeshare.vehicle.repository.RateBandReviewRequestRepository;
import com.routeshare.vehicle.repository.VehicleClassRepository;
import com.routeshare.vehicle.repository.VehicleRateBandFactorRepository;
import com.routeshare.vehicle.repository.VehicleRateBandRepository;
import com.routeshare.vehicle.repository.VehicleRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class RateBandServiceImplTest {
  private static final Instant NOW = Instant.parse("2026-08-01T09:41:00Z");
  private static final long APP_USER_ID = 42L;
  private static final long PROFILE_ID = 7L;
  private static final long VEHICLE_ID = 11L;
  private static final long ADMIN_ID = 1L;

  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identity = mock(IdentityFacade.class);
  private final DriverFacade drivers = mock(DriverFacade.class);
  private final VehicleRepository vehicles = mock(VehicleRepository.class);
  private final VehicleClassRepository classes = mock(VehicleClassRepository.class);
  private final VehicleRateBandRepository bands = mock(VehicleRateBandRepository.class);
  private final VehicleRateBandFactorRepository factors =
      mock(VehicleRateBandFactorRepository.class);
  private final RateBandReviewRequestRepository reviews =
      mock(RateBandReviewRequestRepository.class);
  private final NotificationFacade notifications = mock(NotificationFacade.class);

  private final RateBandServiceImpl service =
      new RateBandServiceImpl(
          current,
          identity,
          drivers,
          vehicles,
          classes,
          bands,
          factors,
          reviews,
          notifications,
          new SimpleMeterRegistry(),
          Clock.fixed(NOW, ZoneOffset.UTC),
          3);

  @BeforeEach
  void setUp() {
    var token = new CurrentUser("subject-1", null, "+94771234567", "Nimali", Set.of("DRIVER"));
    when(current.requireCurrentUser()).thenReturn(token);
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
    when(drivers.findDriverProfileIdByAppUserId(APP_USER_ID)).thenReturn(Optional.of(PROFILE_ID));
    when(drivers.findAppUserIdByDriverProfileId(PROFILE_ID)).thenReturn(Optional.of(APP_USER_ID));
    when(vehicles.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(PROFILE_ID)));
    when(classes.findById("CAR")).thenReturn(Optional.of(carClass()));
    when(bands.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
    when(reviews.save(any()))
        .thenAnswer(
            inv -> {
              RateBandReviewRequestEntity saved = inv.getArgument(0);
              if (saved.getId() == null) {
                saved.setId(31L); // the identity column the database would assign
              }
              return saved;
            });
    when(factors.findByVehicleRateBandIdOrderBySortOrderAsc(anyLong())).thenReturn(List.of());
  }

  private static VehicleEntity vehicle(long driverProfileId) {
    return new VehicleEntity(
        VEHICLE_ID,
        driverProfileId,
        "Toyota",
        "Aqua",
        2018,
        "White",
        "WP CAB-4417",
        3,
        "APPROVED",
        "CAR");
  }

  private static VehicleClassEntity carClass() {
    return VehicleClassEntity.of("CAR", "Car", 3, new BigDecimal("38"), new BigDecimal("62"), 1);
  }

  private static VehicleRateBandEntity withId(VehicleRateBandEntity band) {
    if (band.getId() == null) {
      band.setId(5L);
    }
    return band;
  }

  private VehicleRateBandEntity activeBand(String chosen) {
    var band =
        VehicleRateBandEntity.pendingAssessment(
            VEHICLE_ID, new BigDecimal("41"), new BigDecimal("58"));
    band.setId(5L);
    band.setStatus(VehicleRateBandEntity.STATUS_ACTIVE);
    band.setChosenRate(chosen == null ? null : new BigDecimal(chosen));
    band.setSetAt(NOW);
    return band;
  }

  private static RateBandAssessmentCommand assessment(String min, String max) {
    return new RateBandAssessmentCommand(
        new BigDecimal(min),
        new BigDecimal(max),
        List.of(
            new RateBandAssessmentCommand.FactorCommand(
                "AGE", "Wear and tyres", "2018 · market value LKR 3.4 M", new BigDecimal("-2"))),
        "note");
  }

  // ── the band is assessed, never typed by the driver ──────────────────────────────────────────

  @Test
  void assessmentActivatesTheBandAndDefaultsTheRateToTheMidpoint() {
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.empty());

    var result = service.assess(VEHICLE_ID, assessment("41", "58"), ADMIN_ID);

    assertThat(result.status()).isEqualTo(VehicleRateBandEntity.STATUS_ACTIVE);
    // Midpoint rather than null: the vehicle is publishable the moment the band lands, without
    // waiting for a second driver action.
    assertThat(result.chosenRate()).isEqualByComparingTo("49.50");
  }

  @Test
  void aBandOutsideTheClassRangeIsRefused() {
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.assess(VEHICLE_ID, assessment("30", "58"), ADMIN_ID))
        .isInstanceOf(GateConflictException.class)
        .extracting(ex -> ((GateConflictException) ex).code())
        .isEqualTo(RateBandCodes.BAND_OUTSIDE_CLASS);
  }

  @Test
  void anAssessmentKeepsARateThatStillFitsTheNewBand() {
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(activeBand("50")));

    var result = service.assess(VEHICLE_ID, assessment("45", "60"), ADMIN_ID);

    assertThat(result.chosenRate()).isEqualByComparingTo("50");
  }

  @Test
  void anAssessmentReplacesARateThatNoLongerFits() {
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(activeBand("42")));

    var result = service.assess(VEHICLE_ID, assessment("50", "60"), ADMIN_ID);

    assertThat(result.chosenRate()).isEqualByComparingTo("55.00");
  }

  @Test
  void theAssessingAdminIsNeverNamedToTheDriver() {
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(activeBand("50")));

    assertThat(service.myBand(VEHICLE_ID).setBy()).isEqualTo("ComiGo");
  }

  @Test
  void theDriverIsToldWhenTheirRateIsSet() {
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.empty());

    service.assess(VEHICLE_ID, assessment("41", "58"), ADMIN_ID);

    verify(notifications).notifyUser(anyLong(), any(), any(), any(), any());
  }

  // ── the driver chooses inside the band, and only inside it ───────────────────────────────────

  @Test
  void aRateInsideTheBandIsAccepted() {
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(activeBand("50")));

    var result = service.chooseRate(VEHICLE_ID, new BigDecimal("46"));

    assertThat(result.chosenRate()).isEqualByComparingTo("46");
  }

  @Test
  void aRateAboveTheBandIsRefused() {
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(activeBand("50")));

    assertThatThrownBy(() -> service.chooseRate(VEHICLE_ID, new BigDecimal("59")))
        .isInstanceOf(GateConflictException.class)
        .extracting(ex -> ((GateConflictException) ex).code())
        .isEqualTo(RateBandCodes.RATE_OUTSIDE_BAND);
  }

  @Test
  void aRateBelowTheBandIsRefused() {
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(activeBand("50")));

    assertThatThrownBy(() -> service.chooseRate(VEHICLE_ID, new BigDecimal("40")))
        .extracting(ex -> ((GateConflictException) ex).code())
        .isEqualTo(RateBandCodes.RATE_OUTSIDE_BAND);
  }

  @Test
  void noRateCanBeChosenBeforeTheBandIsAssessed() {
    var pending =
        VehicleRateBandEntity.pendingAssessment(
            VEHICLE_ID, new BigDecimal("38"), new BigDecimal("62"));
    pending.setId(5L);
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(pending));

    assertThatThrownBy(() -> service.chooseRate(VEHICLE_ID, new BigDecimal("50")))
        .extracting(ex -> ((GateConflictException) ex).code())
        .isEqualTo(GateCodes.RATE_BAND_NOT_SET);
  }

  @Test
  void aDriverCannotTouchAnotherDriversBand() {
    when(vehicles.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(99L)));

    assertThatThrownBy(() -> service.chooseRate(VEHICLE_ID, new BigDecimal("50")))
        .isInstanceOf(AccessDeniedException.class);
  }

  // ── one re-assessment, and the live band keeps working while it is open ──────────────────────

  @Test
  void aReviewRequestLeavesTheBandLive() {
    var band = activeBand("50");
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(band));
    when(reviews.findByVehicleIdAndStatus(VEHICLE_ID, RateBandReviewRequestEntity.STATUS_OPEN))
        .thenReturn(Optional.empty());

    var result =
        service.requestReview(
            VEHICLE_ID, new RateBandReviewRequestCommand("NEW_TYRES", "New tyres fitted"));

    assertThat(result.status()).isEqualTo(RateBandReviewRequestEntity.STATUS_OPEN);
    // UNDER_REVIEW still counts as live: asking is not losing the rate you already have.
    assertThat(band.getStatus()).isEqualTo(VehicleRateBandEntity.STATUS_UNDER_REVIEW);
    assertThat(band.isActive()).isTrue();
  }

  @Test
  void aSecondReviewRequestIsRefusedWhileOneIsOpen() {
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(activeBand("50")));
    when(reviews.findByVehicleIdAndStatus(VEHICLE_ID, RateBandReviewRequestEntity.STATUS_OPEN))
        .thenReturn(
            Optional.of(RateBandReviewRequestEntity.open(VEHICLE_ID, APP_USER_ID, "AGAIN", null)));

    assertThatThrownBy(
            () ->
                service.requestReview(VEHICLE_ID, new RateBandReviewRequestCommand("AGAIN", null)))
        .extracting(ex -> ((GateConflictException) ex).code())
        .isEqualTo(RateBandCodes.RATE_REVIEW_ALREADY_OPEN);
  }

  @Test
  void rejectingAReviewLeavesTheLiveBandExactlyAsItWas() {
    var band = activeBand("50");
    band.setStatus(VehicleRateBandEntity.STATUS_UNDER_REVIEW);
    var request = RateBandReviewRequestEntity.open(VEHICLE_ID, APP_USER_ID, "NEW_TYRES", null);
    request.setId(31L);
    when(reviews.findById(31L)).thenReturn(Optional.of(request));
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(band));

    var result =
        service.decideReview(
            31L,
            new RateBandReviewDecisionCommand("REJECTED", null, null, null, "No change"),
            ADMIN_ID);

    assertThat(result.status()).isEqualTo(RateBandReviewRequestEntity.STATUS_REJECTED);
    assertThat(band.getRateMin()).isEqualByComparingTo("41");
    assertThat(band.getRateMax()).isEqualByComparingTo("58");
    assertThat(band.getStatus()).isEqualTo(VehicleRateBandEntity.STATUS_ACTIVE);
  }

  @Test
  void approvingAReviewWithNewNumbersWritesTheNewBand() {
    var band = activeBand("50");
    band.setStatus(VehicleRateBandEntity.STATUS_UNDER_REVIEW);
    var request = RateBandReviewRequestEntity.open(VEHICLE_ID, APP_USER_ID, "NEW_TYRES", null);
    request.setId(31L);
    when(reviews.findById(31L)).thenReturn(Optional.of(request));
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(band));

    service.decideReview(
        31L,
        new RateBandReviewDecisionCommand(
            "APPROVED", new BigDecimal("44"), new BigDecimal("60"), List.of(), "Tyres verified"),
        ADMIN_ID);

    assertThat(band.getRateMin()).isEqualByComparingTo("44");
    assertThat(band.getRateMax()).isEqualByComparingTo("60");
    assertThat(band.getStatus()).isEqualTo(VehicleRateBandEntity.STATUS_ACTIVE);
  }

  @Test
  void anAlreadyDecidedReviewCannotBeDecidedTwice() {
    var request = RateBandReviewRequestEntity.open(VEHICLE_ID, APP_USER_ID, "NEW_TYRES", null);
    request.setId(31L);
    request.decide(RateBandReviewRequestEntity.STATUS_REJECTED, NOW, ADMIN_ID, "done");
    when(reviews.findById(31L)).thenReturn(Optional.of(request));

    assertThatThrownBy(
            () ->
                service.decideReview(
                    31L,
                    new RateBandReviewDecisionCommand("APPROVED", null, null, null, null),
                    ADMIN_ID))
        .isInstanceOf(GateConflictException.class);
    verify(bands, never()).save(any());
  }

  // ── D40: an approved vehicle always has somewhere to point ───────────────────────────────────

  @Test
  void approvalCreatesTheBandRowD40Renders() {
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.empty());

    service.ensureBandExists(VEHICLE_ID);

    verify(bands).save(any(VehicleRateBandEntity.class));
  }

  @Test
  void ensureBandExistsDoesNotOverwriteAnAssessedBand() {
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(activeBand("50")));

    service.ensureBandExists(VEHICLE_ID);

    verify(bands, never()).save(any());
  }

  @Test
  void theBandPayloadCarriesEverythingTheScreensNeed() {
    when(bands.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(activeBand("50")));

    var response = service.myBand(VEHICLE_ID);

    assertThat(response.vehicleLabel()).isEqualTo("Toyota · WP CAB-4417");
    assertThat(response.classBand().min()).isEqualByComparingTo("38");
    assertThat(response.classBand().max()).isEqualByComparingTo("62");
    assertThat(response.band().min()).isEqualByComparingTo("41");
    assertThat(response.position().key()).isEqualTo("mid");
    assertThat(response.positions()).hasSize(3);
    assertThat(response.netEffect()).isNotNull();
  }
}
