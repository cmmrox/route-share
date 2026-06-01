package com.routeshare.vehicle.facade;

public interface VehicleFacade {
  boolean existsApprovedOwnedVehicleWithCapacity(long vehicleId, long driverProfileId, int seats);

  boolean existsByVehicleIdAndDriverProfileId(long vehicleId, long driverProfileId);
}
