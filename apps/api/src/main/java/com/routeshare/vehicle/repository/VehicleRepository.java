package com.routeshare.vehicle.repository;

import com.routeshare.vehicle.dto.request.VehicleRequest;
import com.routeshare.vehicle.dto.response.VehicleResponse;
import com.routeshare.vehicle.entity.VehicleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<VehicleEntity, Long> {
  List<VehicleEntity> findByDriverProfileIdOrderByIdDesc(long driverProfileId);

  boolean existsByIdAndDriverProfileId(long vehicleId, long driverProfileId);

  boolean existsByIdAndDriverProfileIdAndStatusAndSeatCountGreaterThanEqual(
      long vehicleId, long driverProfileId, String status, int seats);

  default VehicleResponse create(long driverProfileId, VehicleRequest request) {
    return toResponse(
        save(
            new VehicleEntity(
                null,
                driverProfileId,
                request.make(),
                request.model(),
                request.manufactureYear(),
                request.color(),
                request.registrationNumber(),
                request.seatCount(),
                null)));
  }

  default List<VehicleResponse> listByDriverProfileId(long driverProfileId) {
    return findByDriverProfileIdOrderByIdDesc(driverProfileId).stream()
        .map(this::toResponse)
        .toList();
  }

  default boolean existsApprovedOwnedVehicleWithCapacity(
      long vehicleId, long driverProfileId, int seats) {
    return existsByIdAndDriverProfileIdAndStatusAndSeatCountGreaterThanEqual(
        vehicleId, driverProfileId, "APPROVED", seats);
  }

  default boolean existsByVehicleIdAndDriverProfileId(long vehicleId, long driverProfileId) {
    return existsByIdAndDriverProfileId(vehicleId, driverProfileId);
  }

  default VehicleResponse review(long vehicleId, String status) {
    VehicleEntity entity = findById(vehicleId).orElseThrow();
    entity.setStatus(status);
    return toResponse(save(entity));
  }

  private VehicleResponse toResponse(VehicleEntity entity) {
    return new VehicleResponse(
        entity.getId(),
        entity.getMake(),
        entity.getModel(),
        entity.getManufactureYear(),
        entity.getColor(),
        entity.getRegistrationNumber(),
        entity.getSeatCount(),
        entity.getStatus());
  }
}
