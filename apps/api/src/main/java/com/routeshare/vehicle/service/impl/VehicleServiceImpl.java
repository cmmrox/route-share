package com.routeshare.vehicle.service.impl;

import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.vehicle.domain.RateBandCodes;
import com.routeshare.vehicle.domain.VehicleReviewStatus;
import com.routeshare.vehicle.dto.request.VehicleRequest;
import com.routeshare.vehicle.dto.response.VehicleResponse;
import com.routeshare.vehicle.entity.VehicleClassEntity;
import com.routeshare.vehicle.entity.VehicleEntity;
import com.routeshare.vehicle.entity.VehicleRateBandEntity;
import com.routeshare.vehicle.mapper.VehicleMapper;
import com.routeshare.vehicle.repository.VehicleClassRepository;
import com.routeshare.vehicle.repository.VehicleRateBandRepository;
import com.routeshare.vehicle.repository.VehicleRepository;
import com.routeshare.vehicle.service.RateBandService;
import com.routeshare.vehicle.service.VehicleService;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final DriverFacade driverFacade;
  private final VehicleRepository vehicles;
  private final VehicleClassRepository classes;
  private final VehicleRateBandRepository bands;
  private final RateBandService rateBands;
  private final VehicleMapper vehicleMapper;

  @Transactional
  public VehicleResponse create(VehicleRequest req) {
    long driverProfileId = currentDriverProfileId();
    VehicleClassEntity vehicleClass = requireClass(req.vehicleClass());
    requireSeatsWithinClass(req.seatCount(), vehicleClass);
    return toResponse(vehicles.save(vehicleMapper.toEntity(driverProfileId, req)));
  }

  public List<VehicleResponse> listMine() {
    return vehicles.findByDriverProfileIdOrderByIdDesc(currentDriverProfileId()).stream()
        .map(this::toResponse)
        .toList();
  }

  public VehicleResponse getMine(long vehicleId) {
    return toResponse(requireOwned(vehicleId));
  }

  @Transactional
  public VehicleResponse updateMine(long vehicleId, VehicleRequest req) {
    var entity = requireOwned(vehicleId);
    VehicleClassEntity vehicleClass = requireClass(req.vehicleClass());
    requireSeatsWithinClass(req.seatCount(), vehicleClass);
    entity.setMake(req.make());
    entity.setModel(req.model());
    entity.setManufactureYear(req.manufactureYear());
    entity.setColor(req.color());
    entity.setRegistrationNumber(req.registrationNumber());
    entity.setSeatCount(req.seatCount());
    entity.setClassKey(vehicleClass.getClassKey());
    return toResponse(vehicles.save(entity));
  }

  @Transactional
  public void deleteMine(long vehicleId) {
    vehicles.delete(requireOwned(vehicleId));
  }

  /**
   * Approval is also where the band's own lifecycle starts. Without the row created here an
   * approved vehicle has nothing to show on D40, and the driver is left guessing why they still
   * cannot publish.
   */
  @Transactional
  public VehicleResponse review(long vehicleId, VehicleReviewStatus status) {
    var entity = vehicles.findById(vehicleId).orElseThrow();
    entity.setStatus(status.name());
    var saved = vehicles.save(entity);
    if (status == VehicleReviewStatus.APPROVED) {
      rateBands.ensureBandExists(vehicleId);
    }
    return toResponse(saved);
  }

  private void requireSeatsWithinClass(int seatCount, VehicleClassEntity vehicleClass) {
    if (seatCount > vehicleClass.getMaxPassengerSeats()) {
      throw new GateConflictException(
          RateBandCodes.SEATS_EXCEED_CLASS_CAP,
          "A %s seats up to %d riders."
              .formatted(vehicleClass.getLabel(), vehicleClass.getMaxPassengerSeats()),
          "/driver/vehicles");
    }
  }

  private VehicleResponse toResponse(VehicleEntity entity) {
    Optional<VehicleRateBandEntity> band = bands.findByVehicleId(entity.getId());
    var base = vehicleMapper.toResponse(entity);
    return new VehicleResponse(
        base.id(),
        base.make(),
        base.model(),
        base.manufactureYear(),
        base.color(),
        base.registrationNumber(),
        base.seatCount(),
        base.status(),
        base.classKey(),
        band.map(VehicleRateBandEntity::getStatus).orElse(VehicleRateBandEntity.STATUS_NOT_SET),
        band.map(VehicleRateBandEntity::getChosenRate).orElse(null));
  }

  private VehicleEntity requireOwned(long vehicleId) {
    long driverProfileId = currentDriverProfileId();
    return vehicles
        .findById(vehicleId)
        .filter(vehicle -> vehicle.getDriverProfileId().equals(driverProfileId))
        .orElseThrow();
  }

  private VehicleClassEntity requireClass(String classKey) {
    return classes
        .findById(classKey == null ? "" : classKey.toUpperCase(Locale.ROOT))
        .orElseThrow(() -> new NoSuchElementException("Unknown vehicle class: " + classKey));
  }

  private long currentDriverProfileId() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return driverFacade
        .findDriverProfileIdByAppUserId(app.appUserId())
        .orElseThrow(() -> new AccessDeniedException("Driver profile is required"));
  }
}
