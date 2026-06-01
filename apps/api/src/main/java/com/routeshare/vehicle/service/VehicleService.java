package com.routeshare.vehicle.service;

import com.routeshare.vehicle.domain.VehicleReviewStatus;
import com.routeshare.vehicle.dto.request.VehicleRequest;
import com.routeshare.vehicle.dto.response.VehicleResponse;
import java.util.List;

public interface VehicleService {
  VehicleResponse create(VehicleRequest req);

  List<VehicleResponse> listMine();

  VehicleResponse review(long vehicleId, VehicleReviewStatus status);
}
