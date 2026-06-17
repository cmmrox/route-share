package com.routeshare.vehicle.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.storage.config.ObjectStorageProperties;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.service.ObjectStoragePort;
import com.routeshare.vehicle.facade.VehicleFacade;
import com.routeshare.vehicle.repository.VehicleDocumentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class VehicleDocumentServiceTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final DriverFacade driverFacade = mock(DriverFacade.class);
  private final VehicleFacade vehicleFacade = mock(VehicleFacade.class);
  private final VehicleDocumentRepository documents = mock(VehicleDocumentRepository.class);
  private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
  private final ObjectStorageProperties props =
      new ObjectStorageProperties(
          true, "http://localhost:9000", "us-east-1", "bucket", "ak", "sk", true, 900);
  private final DomainEventPublisher events = mock(DomainEventPublisher.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-06-18T00:00:00Z"), ZoneOffset.UTC);

  private final VehicleDocumentServiceImpl service =
      new VehicleDocumentServiceImpl(
          current,
          identityFacade,
          driverFacade,
          vehicleFacade,
          documents,
          org.mapstruct.factory.Mappers.getMapper(
              com.routeshare.vehicle.mapper.VehicleMapper.class),
          storage,
          props,
          events,
          clock);

  @BeforeEach
  void setUp() {
    CurrentUser user =
        new CurrentUser("subject", "driver@example.test", null, "Driver", Set.of("DRIVER"));
    AppUser appUser =
        new AppUser(
            42L, UUID.randomUUID(), "subject", "driver@example.test", null, "Driver", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
    when(driverFacade.findDriverProfileIdByAppUserId(42L)).thenReturn(Optional.of(7L));
  }

  @Test
  void listRequiresVehicleOwnedByCurrentDriver() {
    when(vehicleFacade.existsByVehicleIdAndDriverProfileId(99L, 7L)).thenReturn(false);

    assertThatThrownBy(() -> service.listMine(99L))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Vehicle does not belong");

    verify(documents, never()).findByVehicleIdOrderByIdDesc(99L);
  }

  @Test
  void createUploadUrlRequiresOwnedVehicle() {
    when(vehicleFacade.existsByVehicleIdAndDriverProfileId(99L, 7L)).thenReturn(false);
    var req = new UploadUrlRequest("INSURANCE", "application/pdf", 2048, "ins.pdf");

    assertThatThrownBy(() -> service.createUploadUrl(99L, req))
        .isInstanceOf(AccessDeniedException.class);

    verify(documents, never()).save(org.mockito.ArgumentMatchers.any());
  }
}
