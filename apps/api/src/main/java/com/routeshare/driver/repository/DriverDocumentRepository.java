package com.routeshare.driver.repository;

import com.routeshare.driver.dto.request.DocumentMetadataRequest;
import com.routeshare.driver.dto.response.DriverDocumentResponse;
import com.routeshare.driver.entity.DriverDocumentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverDocumentRepository extends JpaRepository<DriverDocumentEntity, Long> {
  List<DriverDocumentEntity> findByDriverProfileIdOrderByIdDesc(long driverProfileId);

  default DriverDocumentResponse create(long driverProfileId, DocumentMetadataRequest request) {
    return toResponse(
        save(
            new DriverDocumentEntity(
                null,
                driverProfileId,
                request.documentType(),
                request.storageKey(),
                null,
                null,
                null)));
  }

  default List<DriverDocumentResponse> list(long driverProfileId) {
    return findByDriverProfileIdOrderByIdDesc(driverProfileId).stream()
        .map(this::toResponse)
        .toList();
  }

  private DriverDocumentResponse toResponse(DriverDocumentEntity entity) {
    return new DriverDocumentResponse(
        entity.getId(),
        entity.getDocumentType(),
        entity.getStorageKey(),
        entity.getStatus(),
        entity.getRejectionReason(),
        entity.getCreatedAt());
  }
}
