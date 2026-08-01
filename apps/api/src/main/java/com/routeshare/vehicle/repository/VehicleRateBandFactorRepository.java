package com.routeshare.vehicle.repository;

import com.routeshare.vehicle.entity.VehicleRateBandFactorEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRateBandFactorRepository
    extends JpaRepository<VehicleRateBandFactorEntity, Long> {
  List<VehicleRateBandFactorEntity> findByVehicleRateBandIdOrderBySortOrderAsc(
      long vehicleRateBandId);

  void deleteByVehicleRateBandId(long vehicleRateBandId);
}
