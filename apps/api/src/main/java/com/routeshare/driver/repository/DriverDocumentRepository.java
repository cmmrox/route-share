package com.routeshare.driver.repository;

import com.routeshare.driver.entity.DriverDocumentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverDocumentRepository extends JpaRepository<DriverDocumentEntity, Long> {
  List<DriverDocumentEntity> findByDriverProfileIdOrderByIdDesc(long driverProfileId);

  Optional<DriverDocumentEntity> findByIdAndDriverProfileId(long id, long driverProfileId);
}
