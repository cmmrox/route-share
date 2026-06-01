package com.routeshare.vehicle.service.impl;

import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.driver.facade.DriverFacade;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.vehicle.domain.VehicleReviewStatus;
import com.routeshare.vehicle.dto.request.VehicleRequest;
import com.routeshare.vehicle.dto.response.VehicleResponse;
import com.routeshare.vehicle.mapper.VehicleMapper;
import com.routeshare.vehicle.repository.VehicleRepository;
import com.routeshare.vehicle.service.VehicleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final DriverFacade driverFacade;
  private final VehicleRepository vehicles;
  private final VehicleMapper vehicleMapper;

  @Transactional
  public VehicleResponse create(VehicleRequest req) {
    long driverProfileId = currentDriverProfileId();
    return vehicleMapper.toResponse(vehicles.save(vehicleMapper.toEntity(driverProfileId, req)));
  }

  public List<VehicleResponse> listMine() {
    return vehicles.findByDriverProfileIdOrderByIdDesc(currentDriverProfileId()).stream()
        .map(vehicleMapper::toResponse)
        .toList();
  }

  @Transactional
  public VehicleResponse review(long vehicleId, VehicleReviewStatus status) {
    var entity = vehicles.findById(vehicleId).orElseThrow();
    entity.setStatus(status.name());
    return vehicleMapper.toResponse(vehicles.save(entity));
  }

  private long currentDriverProfileId() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return driverFacade
        .findDriverProfileIdByAppUserId(app.appUserId())
        .orElseThrow(() -> new AccessDeniedException("Driver profile is required"));
  }
}
