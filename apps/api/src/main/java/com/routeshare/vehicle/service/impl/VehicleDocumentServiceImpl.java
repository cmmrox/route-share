package com.routeshare.vehicle.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.vehicle.dto.request.VehicleDocumentRequest;
import com.routeshare.vehicle.dto.response.VehicleDocumentResponse;
import com.routeshare.vehicle.facade.VehicleFacade;
import com.routeshare.vehicle.mapper.VehicleMapper;
import com.routeshare.vehicle.repository.VehicleDocumentRepository;
import com.routeshare.vehicle.service.VehicleDocumentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleDocumentServiceImpl implements VehicleDocumentService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final DriverFacade driverFacade;
  private final VehicleFacade vehicleFacade;
  private final VehicleDocumentRepository documents;
  private final VehicleMapper vehicleMapper;

  @Transactional
  public VehicleDocumentResponse create(long vehicleId, VehicleDocumentRequest req) {
    requireOwnedVehicle(vehicleId);
    return vehicleMapper.toDocumentResponse(
        documents.save(vehicleMapper.toDocumentEntity(vehicleId, req)));
  }

  public List<VehicleDocumentResponse> list(long vehicleId) {
    requireOwnedVehicle(vehicleId);
    return documents.findByVehicleIdOrderByIdDesc(vehicleId).stream()
        .map(vehicleMapper::toDocumentResponse)
        .toList();
  }

  public List<VehicleDocumentResponse> listMine(long vehicleId) {
    return list(vehicleId);
  }

  private void requireOwnedVehicle(long vehicleId) {
    long driverProfileId = currentDriverProfileId();
    if (!vehicleFacade.existsByVehicleIdAndDriverProfileId(vehicleId, driverProfileId)) {
      throw new AccessDeniedException("Vehicle does not belong to current driver");
    }
  }

  private long currentDriverProfileId() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return driverFacade
        .findDriverProfileIdByAppUserId(app.appUserId())
        .orElseThrow(() -> new AccessDeniedException("Driver profile is required"));
  }
}
