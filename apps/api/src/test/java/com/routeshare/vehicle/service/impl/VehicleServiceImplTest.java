package com.routeshare.vehicle.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.vehicle.domain.RateBandCodes;
import com.routeshare.vehicle.domain.VehicleReviewStatus;
import com.routeshare.vehicle.dto.request.VehicleRequest;
import com.routeshare.vehicle.dto.response.VehicleResponse;
import com.routeshare.vehicle.entity.VehicleClassEntity;
import com.routeshare.vehicle.entity.VehicleEntity;
import com.routeshare.vehicle.mapper.VehicleMapper;
import com.routeshare.vehicle.repository.VehicleClassRepository;
import com.routeshare.vehicle.repository.VehicleRateBandRepository;
import com.routeshare.vehicle.repository.VehicleRepository;
import com.routeshare.vehicle.service.RateBandService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class VehicleServiceImplTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final DriverFacade driverFacade = mock(DriverFacade.class);
  private final VehicleRepository vehicles = mock(VehicleRepository.class);
  private final VehicleClassRepository classes = mock(VehicleClassRepository.class);
  private final VehicleRateBandRepository bands = mock(VehicleRateBandRepository.class);
  private final RateBandService rateBands = mock(RateBandService.class);
  private final VehicleMapper mapper = mock(VehicleMapper.class);
  private final VehicleServiceImpl service =
      new VehicleServiceImpl(
          current, identityFacade, driverFacade, vehicles, classes, bands, rateBands, mapper);

  @BeforeEach
  void setUp() {
    var user = new CurrentUser("sub", "driver@example.test", null, "Driver", Set.of("DRIVER"));
    var appUser =
        new AppUser(7L, UUID.randomUUID(), "sub", "driver@example.test", null, "Driver", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
    when(driverFacade.findDriverProfileIdByAppUserId(7L)).thenReturn(Optional.of(77L));
    when(classes.findById("CAR")).thenReturn(Optional.of(vehicleClass("CAR", "Car", 4)));
  }

  private static VehicleClassEntity vehicleClass(String key, String label, int maxSeats) {
    return VehicleClassEntity.of(
        key, label, maxSeats, new BigDecimal("38"), new BigDecimal("62"), 1);
  }

  @Test
  void createPersistsVehicleForCurrentDriverProfile() {
    var req = request("CAR-001");
    var entity = entity(11L, 77L, "Toyota", "Aqua", "CAR-001");
    when(mapper.toEntity(77L, req)).thenReturn(entity);
    when(vehicles.save(entity)).thenReturn(entity);
    when(mapper.toResponse(entity)).thenReturn(response(11L, "CAR-001", "PENDING"));

    var created = service.create(req);

    assertThat(created.id()).isEqualTo(11L);
    verify(mapper).toEntity(77L, req);
  }

  @Test
  void listMineMapsOnlyCurrentDriverVehicles() {
    var entity = entity(12L, 77L, "Honda", "Fit", "CAR-002");
    when(vehicles.findByDriverProfileIdOrderByIdDesc(77L)).thenReturn(List.of(entity));
    when(mapper.toResponse(entity)).thenReturn(response(12L, "CAR-002", "APPROVED"));

    assertThat(service.listMine())
        .extracting(VehicleResponse::registrationNumber)
        .containsExactly("CAR-002");
  }

  @Test
  void getMineRejectsVehicleOwnedByAnotherDriver() {
    when(vehicles.findById(99L)).thenReturn(Optional.of(entity(99L, 88L, "Other", "Car", "OTHER")));

    assertThatThrownBy(() -> service.getMine(99L)).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void updateMineChangesOwnedVehicleFields() {
    var req = request("NEW-123");
    var existing = entity(14L, 77L, "Old", "Old", "OLD-123");
    when(vehicles.findById(14L)).thenReturn(Optional.of(existing));
    when(vehicles.save(existing)).thenReturn(existing);
    when(mapper.toResponse(existing)).thenReturn(response(14L, "NEW-123", "PENDING"));

    var updated = service.updateMine(14L, req);

    assertThat(existing.getRegistrationNumber()).isEqualTo("NEW-123");
    assertThat(updated.registrationNumber()).isEqualTo("NEW-123");
  }

  @Test
  void deleteMineDeletesOnlyOwnedVehicle() {
    var existing = entity(15L, 77L, "Old", "Old", "OLD-123");
    when(vehicles.findById(15L)).thenReturn(Optional.of(existing));

    service.deleteMine(15L);

    verify(vehicles).delete(existing);
  }

  @Test
  void reviewUpdatesVerificationStatus() {
    var existing = entity(20L, 77L, "Honda", "Fit", "CAR-020");
    when(vehicles.findById(20L)).thenReturn(Optional.of(existing));
    when(vehicles.save(existing)).thenReturn(existing);
    when(mapper.toResponse(existing)).thenReturn(response(20L, "CAR-020", "APPROVED"));

    var reviewed = service.review(20L, VehicleReviewStatus.APPROVED);

    assertThat(existing.getStatus()).isEqualTo("APPROVED");
    assertThat(reviewed.status()).isEqualTo("APPROVED");
  }

  @Test
  void createRequiresDriverProfile() {
    when(driverFacade.findDriverProfileIdByAppUserId(7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(request("CAR-403")))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Driver profile is required");
  }

  @Test
  void seatCountAboveTheClassCapIsRefused() {
    when(classes.findById("CAR")).thenReturn(Optional.of(vehicleClass("CAR", "Car", 3)));

    // Selling a fourth seat in a three-seat class is a capacity lie told to a rider.
    assertThatThrownBy(() -> service.create(request("CAR-004")))
        .isInstanceOf(GateConflictException.class)
        .extracting(ex -> ((GateConflictException) ex).code())
        .isEqualTo(RateBandCodes.SEATS_EXCEED_CLASS_CAP);
  }

  @Test
  void approvalStartsTheRateBandLifecycle() {
    var existing = entity(21L, 77L, "Honda", "Fit", "CAR-021");
    when(vehicles.findById(21L)).thenReturn(Optional.of(existing));
    when(vehicles.save(existing)).thenReturn(existing);
    when(mapper.toResponse(existing)).thenReturn(response(21L, "CAR-021", "APPROVED"));

    service.review(21L, VehicleReviewStatus.APPROVED);

    // Without this row D40 has nothing to show and the driver cannot see why they cannot publish.
    verify(rateBands).ensureBandExists(21L);
  }

  private static VehicleRequest request(String registration) {
    return new VehicleRequest("Toyota", "Aqua", 2020, "White", registration, 4, "CAR");
  }

  private static VehicleEntity entity(
      long id, long driverProfileId, String make, String model, String registration) {
    return new VehicleEntity(
        id, driverProfileId, make, model, 2020, "White", registration, 4, "PENDING", "CAR");
  }

  private static VehicleResponse response(long id, String registration, String status) {
    return new VehicleResponse(
        id, "Toyota", "Aqua", 2020, "White", registration, 4, status, "CAR", null, null);
  }
}
