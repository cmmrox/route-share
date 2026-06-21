package com.routeshare.driver.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.entity.DriverDocumentEntity;
import com.routeshare.driver.repository.DriverDocumentRepository;
import com.routeshare.driver.repository.DriverProfileRepository;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.vehicle.facade.VehicleFacade;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DriverVerificationServiceImplTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final DriverProfileRepository drivers = mock(DriverProfileRepository.class);
  private final DriverDocumentRepository documents = mock(DriverDocumentRepository.class);
  private final VehicleFacade vehicleFacade = mock(VehicleFacade.class);
  private final DriverVerificationServiceImpl service =
      new DriverVerificationServiceImpl(current, identityFacade, drivers, documents, vehicleFacade);

  @BeforeEach
  void setUp() {
    var user = new CurrentUser("sub", "d@test", null, "D", Set.of("DRIVER"));
    var appUser = new AppUser(5L, UUID.randomUUID(), "sub", "d@test", null, "D", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
  }

  private static DriverDocumentEntity doc(long id, String type, String status) {
    var e = DriverDocumentEntity.awaitingUpload(7L, type, "key", "image/jpeg", 10L, "f.jpg");
    e.setId(id);
    e.setStatus(status);
    return e;
  }

  @Test
  void notStartedWhenNoDriverProfile() {
    when(drivers.findIdByAppUserId(5L)).thenReturn(Optional.empty());

    var res = service.status();

    assertThat(res.profileStatus()).isEqualTo("NOT_STARTED");
    assertThat(res.ready()).isFalse();
    assertThat(res.documents()).extracting("status").containsOnly("MISSING");
  }

  @Test
  void readyWhenProfileApprovedDocsApprovedAndVehicleApproved() {
    when(drivers.findIdByAppUserId(5L)).thenReturn(Optional.of(7L));
    when(drivers.findStatusByAppUserId(5L)).thenReturn(Optional.of("APPROVED"));
    when(documents.findByDriverProfileIdOrderByIdDesc(7L))
        .thenReturn(
            List.of(
                doc(2L, "IDENTITY", DriverDocumentEntity.STATUS_APPROVED),
                doc(1L, "LICENCE", DriverDocumentEntity.STATUS_APPROVED)));
    when(vehicleFacade.existsApprovedVehicleForDriver(7L)).thenReturn(true);

    var res = service.status();

    assertThat(res.ready()).isTrue();
    assertThat(res.hasApprovedVehicle()).isTrue();
  }

  @Test
  void notReadyAndGuidesWhenDocsRejectedOrVehicleMissing() {
    when(drivers.findIdByAppUserId(5L)).thenReturn(Optional.of(7L));
    when(drivers.findStatusByAppUserId(5L)).thenReturn(Optional.of("PENDING_REVIEW"));
    when(documents.findByDriverProfileIdOrderByIdDesc(7L))
        .thenReturn(
            List.of(
                doc(3L, "IDENTITY", DriverDocumentEntity.STATUS_REJECTED),
                doc(2L, "IDENTITY", DriverDocumentEntity.STATUS_APPROVED)));
    when(vehicleFacade.existsApprovedVehicleForDriver(7L)).thenReturn(false);

    var res = service.status();

    assertThat(res.ready()).isFalse();
    // latest IDENTITY (id 3) is REJECTED, LICENCE missing, vehicle missing
    assertThat(res.documents())
        .anySatisfy(
            d -> {
              assertThat(d.documentType()).isEqualTo("IDENTITY");
              assertThat(d.status()).isEqualTo("REJECTED");
            });
    assertThat(res.nextSteps()).isNotEmpty();
  }
}
