package com.routeshare.vehicle.repository;

import com.routeshare.vehicle.entity.VehicleClassEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleClassRepository extends JpaRepository<VehicleClassEntity, String> {
  List<VehicleClassEntity> findByActiveTrueOrderBySortOrderAsc();
}
