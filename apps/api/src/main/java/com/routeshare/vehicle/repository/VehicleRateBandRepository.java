package com.routeshare.vehicle.repository;

import com.routeshare.vehicle.entity.VehicleRateBandEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRateBandRepository extends JpaRepository<VehicleRateBandEntity, Long> {
  Optional<VehicleRateBandEntity> findByVehicleId(long vehicleId);

  List<VehicleRateBandEntity> findByVehicleIdIn(List<Long> vehicleIds);

  /**
   * The publish gate's question, asked in one round trip: does this driver have an approved vehicle
   * whose band is live? An approved vehicle with no band cannot carry a price, so it cannot
   * publish.
   */
  @Query(
      """
      SELECT COUNT(b) > 0 FROM VehicleRateBandEntity b, VehicleEntity v
       WHERE b.vehicleId = v.id
         AND v.driverProfileId = :driverProfileId
         AND v.status = 'APPROVED'
         AND b.status IN ('ACTIVE', 'UNDER_REVIEW')
         AND b.chosenRate IS NOT NULL
      """)
  boolean existsPublishableBandForDriver(@Param("driverProfileId") long driverProfileId);

  long countByStatus(String status);
}
