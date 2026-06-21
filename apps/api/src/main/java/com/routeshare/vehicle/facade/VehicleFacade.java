package com.routeshare.vehicle.facade;

public interface VehicleFacade {
  boolean existsApprovedOwnedVehicleWithCapacity(long vehicleId, long driverProfileId, int seats);

  boolean existsByVehicleIdAndDriverProfileId(long vehicleId, long driverProfileId);

  /** True when the driver has at least one APPROVED vehicle (used for verification readiness). */
  boolean existsApprovedVehicleForDriver(long driverProfileId);
}
