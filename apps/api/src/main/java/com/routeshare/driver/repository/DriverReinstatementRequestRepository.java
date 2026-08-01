package com.routeshare.driver.repository;

import com.routeshare.driver.entity.DriverReinstatementRequestEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverReinstatementRequestRepository
    extends JpaRepository<DriverReinstatementRequestEntity, Long> {
  List<DriverReinstatementRequestEntity> findByDriverProfileIdOrderByIdDesc(long driverProfileId);

  Optional<DriverReinstatementRequestEntity> findByDeactivationIdAndStatus(
      long deactivationId, String status);
}
