package com.routeshare.vehicle.repository;

import com.routeshare.vehicle.dto.request.VehicleDocumentRequest;
import com.routeshare.vehicle.dto.response.VehicleDocumentResponse;
import com.routeshare.vehicle.entity.VehicleDocumentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocumentEntity, Long> {
  List<VehicleDocumentEntity> findByVehicleIdOrderByIdDesc(long vehicleId);

  default VehicleDocumentResponse create(long vehicleId, VehicleDocumentRequest request) {
    return toResponse(
        save(
            new VehicleDocumentEntity(
                null, vehicleId, request.documentType(), request.storageKey(), null, null, null)));
  }

  default List<VehicleDocumentResponse> list(long vehicleId) {
    return findByVehicleIdOrderByIdDesc(vehicleId).stream().map(this::toResponse).toList();
  }

  private VehicleDocumentResponse toResponse(VehicleDocumentEntity entity) {
    return new VehicleDocumentResponse(
        entity.getId(),
        entity.getVehicleId(),
        entity.getDocumentType(),
        entity.getStorageKey(),
        entity.getStatus(),
        entity.getRejectionReason(),
        entity.getCreatedAt());
  }
}
