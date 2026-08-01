package com.routeshare.driver.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.routeshare.common.errors.GateCodes;
import com.routeshare.driver.domain.DriverGate;
import com.routeshare.driver.entity.DriverDocumentEntity;
import com.routeshare.driver.entity.DriverProfileEntity;
import com.routeshare.driver.repository.DriverDeactivationRepository;
import com.routeshare.driver.repository.DriverDocumentRepository;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.vehicle.facade.VehicleFacade;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Every gate code, produced by the condition that is supposed to produce it. */
class DriverGateServiceTest {
  private static final Instant NOW = Instant.parse("2026-08-01T09:41:00Z");
  private static final long APP_USER_ID = 42L;
  private static final long PROFILE_ID = 7L;

  private final DriverProfileRepository drivers = mock(DriverProfileRepository.class);
  private final DriverDeactivationRepository deactivations =
      mock(DriverDeactivationRepository.class);
  private final DriverDocumentRepository documents = mock(DriverDocumentRepository.class);
  private final VehicleFacade vehicles = mock(VehicleFacade.class);

  private final DriverGateServiceImpl service =
      new DriverGateServiceImpl(
          drivers, deactivations, documents, vehicles, Clock.fixed(NOW, ZoneOffset.UTC));

  private void profile(String verificationStatus) {
    var entity = new DriverProfileEntity(PROFILE_ID, APP_USER_ID, "Nimali", verificationStatus);
    when(drivers.findByAppUserId(APP_USER_ID)).thenReturn(Optional.of(entity));
  }

  private void noProfile() {
    when(drivers.findByAppUserId(APP_USER_ID)).thenReturn(Optional.empty());
  }

  private void documents(DriverDocumentEntity... docs) {
    when(documents.findByDriverProfileIdOrderByIdDesc(PROFILE_ID)).thenReturn(List.of(docs));
  }

  private static DriverDocumentEntity document(
      long id, String type, String status, Instant expiry) {
    var doc = DriverDocumentEntity.awaitingUpload(PROFILE_ID, type, "key", "image/jpeg", 10L, "f");
    doc.setId(id);
    doc.setStatus(status);
    doc.setExpiresAt(expiry);
    return doc;
  }

  private static List<String> codes(List<DriverGate> gates) {
    return gates.stream().map(DriverGate::code).toList();
  }

  @Test
  void noProfileMeansTheBecomeADriverGate() {
    noProfile();

    assertThat(codes(service.driveGates(APP_USER_ID)))
        .containsExactly(GateCodes.DRIVER_PROFILE_MISSING);
  }

  @Test
  void submittedProfileIsUnderReview() {
    profile("SUBMITTED");

    assertThat(codes(service.driveGates(APP_USER_ID)))
        .containsExactly(GateCodes.DRIVER_REVIEW_PENDING);
  }

  @Test
  void rejectedProfileNamesTheApplicationNotTheReviewer() {
    profile("REJECTED");

    List<DriverGate> gates = service.driveGates(APP_USER_ID);
    assertThat(codes(gates)).containsExactly(GateCodes.DRIVER_APPLICATION_REJECTED);
    assertThat(gates.get(0).actionPath()).isNotBlank();
  }

  @Test
  void anOpenDeactivationOutranksAnApprovedProfile() {
    profile("APPROVED");
    when(deactivations.existsByDriverProfileIdAndReinstatedAtIsNull(PROFILE_ID)).thenReturn(true);

    List<DriverGate> gates = service.driveGates(APP_USER_ID);
    assertThat(codes(gates)).containsExactly(GateCodes.DRIVER_DEACTIVATED);
    // D34 promises both of these in the same breath; the message is where the driver reads them.
    assertThat(gates.get(0).message()).contains("ride as a passenger").contains("paid out");
  }

  @Test
  void anApprovedUndeactivatedDriverHasNoDriveGate() {
    profile("APPROVED");

    assertThat(service.driveGates(APP_USER_ID)).isEmpty();
    assertThat(service.isDeactivated(APP_USER_ID)).isFalse();
  }

  @Test
  void publishingRepeatsTheDriveGateWhenDrivingItselfIsBlocked() {
    profile("SUBMITTED");

    // Listing document blockers to someone who cannot drive yet would be noise on the wrong screen.
    assertThat(codes(service.publishGates(APP_USER_ID)))
        .containsExactly(GateCodes.DRIVER_REVIEW_PENDING);
  }

  @Test
  void aNeverUploadedDocumentBlocksPublishing() {
    profile("APPROVED");
    documents(document(1L, "IDENTITY", DriverDocumentEntity.STATUS_APPROVED, null));
    when(vehicles.existsApprovedVehicleForDriver(PROFILE_ID)).thenReturn(true);
    when(vehicles.existsPublishableVehicleForDriver(PROFILE_ID)).thenReturn(true);

    List<DriverGate> gates = service.publishGates(APP_USER_ID);
    assertThat(codes(gates)).containsExactly(GateCodes.DOCUMENT_MISSING);
    assertThat(gates.get(0).message()).contains("licence");
  }

  @Test
  void aRejectedDocumentBlocksPublishingAndNamesTheDocumentByType() {
    profile("APPROVED");
    documents(
        document(1L, "IDENTITY", DriverDocumentEntity.STATUS_APPROVED, null),
        document(2L, "LICENCE", DriverDocumentEntity.STATUS_REJECTED, null));
    when(vehicles.existsApprovedVehicleForDriver(PROFILE_ID)).thenReturn(true);
    when(vehicles.existsPublishableVehicleForDriver(PROFILE_ID)).thenReturn(true);

    List<DriverGate> gates = service.publishGates(APP_USER_ID);
    assertThat(codes(gates)).containsExactly(GateCodes.DOCUMENT_REJECTED);
    assertThat(gates.get(0).message()).contains("licence").doesNotContain("2");
  }

  @Test
  void anExpiredDocumentBlocksPublishingEvenThoughItWasApproved() {
    profile("APPROVED");
    documents(
        document(1L, "IDENTITY", DriverDocumentEntity.STATUS_APPROVED, null),
        document(2L, "LICENCE", DriverDocumentEntity.STATUS_APPROVED, NOW.minusSeconds(86_400)));
    when(vehicles.existsApprovedVehicleForDriver(PROFILE_ID)).thenReturn(true);
    when(vehicles.existsPublishableVehicleForDriver(PROFILE_ID)).thenReturn(true);

    assertThat(codes(service.publishGates(APP_USER_ID)))
        .containsExactly(GateCodes.DOCUMENT_EXPIRED);
  }

  @Test
  void anApprovedVehicleWithNoRateBandBlocksPublishing() {
    profile("APPROVED");
    documents(
        document(1L, "IDENTITY", DriverDocumentEntity.STATUS_APPROVED, null),
        document(2L, "LICENCE", DriverDocumentEntity.STATUS_APPROVED, null));
    when(vehicles.existsApprovedVehicleForDriver(PROFILE_ID)).thenReturn(true);
    when(vehicles.existsPublishableVehicleForDriver(PROFILE_ID)).thenReturn(false);

    // Board D40: approved papers are not a price, and the driver is told so rather than left to
    // discover it when publishing fails.
    assertThat(codes(service.publishGates(APP_USER_ID)))
        .containsExactly(GateCodes.RATE_BAND_NOT_SET);
  }

  @Test
  void noApprovedVehicleBlocksPublishing() {
    profile("APPROVED");
    documents(
        document(1L, "IDENTITY", DriverDocumentEntity.STATUS_APPROVED, null),
        document(2L, "LICENCE", DriverDocumentEntity.STATUS_APPROVED, null));
    when(vehicles.existsApprovedVehicleForDriver(PROFILE_ID)).thenReturn(false);

    assertThat(codes(service.publishGates(APP_USER_ID)))
        .containsExactly(GateCodes.VEHICLE_NOT_APPROVED);
  }

  @Test
  void afullyReadyDriverHasNoPublishGate() {
    profile("APPROVED");
    documents(
        document(1L, "IDENTITY", DriverDocumentEntity.STATUS_APPROVED, null),
        document(2L, "LICENCE", DriverDocumentEntity.STATUS_APPROVED, NOW.plusSeconds(86_400)));
    when(vehicles.existsApprovedVehicleForDriver(PROFILE_ID)).thenReturn(true);
    when(vehicles.existsPublishableVehicleForDriver(PROFILE_ID)).thenReturn(true);

    assertThat(service.publishGates(APP_USER_ID)).isEmpty();
  }

  @Test
  void gateMessagesNeverLeakInternalDetail() {
    profile("REJECTED");

    for (DriverGate gate : service.driveGates(APP_USER_ID)) {
      assertThat(gate.message()).doesNotContain("reviewer").doesNotContain("admin");
      assertThat(gate.actionPath()).startsWith("/");
    }
  }
}
