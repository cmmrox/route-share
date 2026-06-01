package com.routeshare.vehicle.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.vehicle.facade.VehicleFacade;
import com.routeshare.vehicle.repository.VehicleDocumentRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class VehicleDocumentServiceTest {
  private final CurrentUserProvider current = org.mockito.Mockito.mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = org.mockito.Mockito.mock(IdentityFacade.class);
  private final DriverFacade driverFacade = org.mockito.Mockito.mock(DriverFacade.class);
  private final VehicleFacade vehicleFacade = org.mockito.Mockito.mock(VehicleFacade.class);
  private final VehicleDocumentRepository documents =
      org.mockito.Mockito.mock(VehicleDocumentRepository.class);
  private final VehicleDocumentServiceImpl service =
      new VehicleDocumentServiceImpl(
          current,
          identityFacade,
          driverFacade,
          vehicleFacade,
          documents,
          org.mapstruct.factory.Mappers.getMapper(
              com.routeshare.vehicle.mapper.VehicleMapper.class));

  @Test
  void listRequiresVehicleOwnedByCurrentDriver() {
    CurrentUser user =
        new CurrentUser("subject", "driver@example.test", null, "Driver", Set.of("DRIVER"));
    AppUser appUser =
        new AppUser(
            42L, UUID.randomUUID(), "subject", "driver@example.test", null, "Driver", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
    when(driverFacade.findDriverProfileIdByAppUserId(42L)).thenReturn(Optional.of(7L));
    when(vehicleFacade.existsByVehicleIdAndDriverProfileId(99L, 7L)).thenReturn(false);

    assertThatThrownBy(() -> service.listMine(99L))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Vehicle does not belong");

    verify(documents, never()).findByVehicleIdOrderByIdDesc(99L);
  }
}
