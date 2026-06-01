package com.routeshare.vehicle.facade.impl;

import com.routeshare.vehicle.facade.VehicleFacade;
import com.routeshare.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleFacadeImpl implements VehicleFacade {
  private final VehicleRepository vehicles;

  @Override
  public boolean existsApprovedOwnedVehicleWithCapacity(
      long vehicleId, long driverProfileId, int seats) {
    return vehicles.existsApprovedOwnedVehicleWithCapacity(vehicleId, driverProfileId, seats);
  }

  @Override
  public boolean existsByVehicleIdAndDriverProfileId(long vehicleId, long driverProfileId) {
    return vehicles.existsByVehicleIdAndDriverProfileId(vehicleId, driverProfileId);
  }
}
