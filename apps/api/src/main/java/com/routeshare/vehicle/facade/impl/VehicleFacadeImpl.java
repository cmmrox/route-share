package com.routeshare.vehicle.facade.impl;

import com.routeshare.vehicle.entity.VehicleRateBandEntity;
import com.routeshare.vehicle.facade.VehicleFacade;
import com.routeshare.vehicle.repository.VehicleRateBandRepository;
import com.routeshare.vehicle.repository.VehicleRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleFacadeImpl implements VehicleFacade {
  private final VehicleRepository vehicles;
  private final VehicleRateBandRepository bands;

  @Override
  public boolean existsApprovedOwnedVehicleWithCapacity(
      long vehicleId, long driverProfileId, int seats) {
    return vehicles.existsApprovedOwnedVehicleWithCapacity(vehicleId, driverProfileId, seats);
  }

  @Override
  public boolean existsByVehicleIdAndDriverProfileId(long vehicleId, long driverProfileId) {
    return vehicles.existsByVehicleIdAndDriverProfileId(vehicleId, driverProfileId);
  }

  @Override
  public boolean existsApprovedVehicleForDriver(long driverProfileId) {
    return vehicles.existsApprovedVehicleForDriver(driverProfileId);
  }

  @Override
  public boolean existsPublishableVehicleForDriver(long driverProfileId) {
    return bands.existsPublishableBandForDriver(driverProfileId);
  }

  @Override
  public Optional<BigDecimal> ratePerKmFor(long vehicleId) {
    return bands
        .findByVehicleId(vehicleId)
        .filter(VehicleRateBandEntity::isActive)
        .map(VehicleRateBandEntity::getChosenRate);
  }

  @Override
  public boolean hasActiveBand(long vehicleId) {
    return bands.findByVehicleId(vehicleId).filter(VehicleRateBandEntity::isActive).isPresent();
  }
}
