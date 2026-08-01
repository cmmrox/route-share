package com.routeshare.vehicle.repository;

import com.routeshare.vehicle.entity.RateBandReviewRequestEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateBandReviewRequestRepository
    extends JpaRepository<RateBandReviewRequestEntity, Long> {
  List<RateBandReviewRequestEntity> findByVehicleIdOrderByIdDesc(long vehicleId);

  Optional<RateBandReviewRequestEntity> findByVehicleIdAndStatus(long vehicleId, String status);

  List<RateBandReviewRequestEntity> findByStatusOrderByIdDesc(String status);
}
