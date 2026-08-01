package com.routeshare.vehicle.repository;

import com.routeshare.vehicle.entity.VehicleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<VehicleEntity, Long> {
  List<VehicleEntity> findByDriverProfileIdOrderByIdDesc(long driverProfileId);

  boolean existsByIdAndDriverProfileId(long vehicleId, long driverProfileId);

  boolean existsByIdAndDriverProfileIdAndStatusAndSeatCountGreaterThanEqual(
      long vehicleId, long driverProfileId, String status, int seats);

  boolean existsByDriverProfileIdAndStatus(long driverProfileId, String status);

  default boolean existsApprovedOwnedVehicleWithCapacity(
      long vehicleId, long driverProfileId, int seats) {
    return existsByIdAndDriverProfileIdAndStatusAndSeatCountGreaterThanEqual(
        vehicleId, driverProfileId, "APPROVED", seats);
  }

  default boolean existsByVehicleIdAndDriverProfileId(long vehicleId, long driverProfileId) {
    return existsByIdAndDriverProfileId(vehicleId, driverProfileId);
  }

  default boolean existsApprovedVehicleForDriver(long driverProfileId) {
    return existsByDriverProfileIdAndStatus(driverProfileId, "APPROVED");
  }
}
