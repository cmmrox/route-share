package com.routeshare.driver.repository;

import com.routeshare.driver.entity.DriverDeactivationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverDeactivationRepository
    extends JpaRepository<DriverDeactivationEntity, Long> {
  /** The open deactivation, if any. At most one exists — the database enforces it. */
  Optional<DriverDeactivationEntity> findByDriverProfileIdAndReinstatedAtIsNull(
      long driverProfileId);

  boolean existsByDriverProfileIdAndReinstatedAtIsNull(long driverProfileId);
}
