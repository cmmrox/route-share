package com.routeshare.vehicle.repository;

import com.routeshare.vehicle.entity.VehicleDocumentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocumentEntity, Long> {
  List<VehicleDocumentEntity> findByVehicleIdOrderByIdDesc(long vehicleId);

  Optional<VehicleDocumentEntity> findByIdAndVehicleId(long id, long vehicleId);
}
