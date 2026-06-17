package com.routeshare.routing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.routing.dto.request.CoordinateRequest;
import com.routeshare.routing.dto.request.RoutePublishRequest;
import com.routeshare.routing.dto.request.RouteSearchRequest;
import com.routeshare.routing.repository.RoutePlanRepository;
import com.routeshare.vehicle.facade.VehicleFacade;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class RouteServiceTest {
  private final CurrentUserProvider current = org.mockito.Mockito.mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = org.mockito.Mockito.mock(IdentityFacade.class);
  private final DriverFacade driverFacade = org.mockito.Mockito.mock(DriverFacade.class);
  private final VehicleFacade vehicleFacade = org.mockito.Mockito.mock(VehicleFacade.class);
  private final RoutePlanRepository routes = org.mockito.Mockito.mock(RoutePlanRepository.class);
  private final RouteServiceImpl service =
      new RouteServiceImpl(
          current,
          identityFacade,
          driverFacade,
          vehicleFacade,
          routes,
          Clock.fixed(Instant.parse("2026-06-01T09:00:00Z"), ZoneOffset.UTC));

  @Test
  void rejectsNonFiniteCoordinates() {
    var coordinates =
        List.of(new CoordinateRequest(6.9, 79.8), new CoordinateRequest(Double.NaN, 80.0));

    assertThatThrownBy(() -> service.validateCoordinates(coordinates))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid route coordinate");
  }

  @Test
  void rejectsRoutesWithoutMovement() {
    var coordinates = List.of(new CoordinateRequest(6.9, 79.8), new CoordinateRequest(6.9, 79.8));

    assertThatThrownBy(() -> service.validateCoordinates(coordinates))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("distinct");
  }

  @Test
  void requiresApprovedDriverProfileBeforeVehicleLookup() {
    var user = new CurrentUser("subject", "driver@example.test", null, "Driver", Set.of("DRIVER"));
    var appUser =
        new AppUser(
            42L, UUID.randomUUID(), "subject", "driver@example.test", null, "Driver", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
    when(driverFacade.findApprovedDriverProfileIdByAppUserId(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.publish(validRequest()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Approved driver profile");

    verify(vehicleFacade, never())
        .existsApprovedOwnedVehicleWithCapacity(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  void routeSearchAllowsIdentityUpsertTransaction() throws NoSuchMethodException {
    var searchMethod = RouteServiceImpl.class.getMethod("search", RouteSearchRequest.class);

    var tx = searchMethod.getAnnotation(Transactional.class);

    assertThat(tx).isNotNull();
    assertThat(tx.readOnly()).isFalse();
  }

  private RoutePublishRequest validRequest() {
    return new RoutePublishRequest(
        10L,
        "Colombo",
        "Kandy",
        List.of(new CoordinateRequest(6.9271, 79.8612), new CoordinateRequest(7.2906, 80.6337)),
        2,
        Instant.parse("2026-06-01T10:00:00Z"));
  }
}
