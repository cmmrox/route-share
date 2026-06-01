package com.routeshare.vehicle.service;

import com.routeshare.vehicle.dto.request.VehicleDocumentRequest;
import com.routeshare.vehicle.dto.response.VehicleDocumentResponse;
import java.util.List;

public interface VehicleDocumentService {
  VehicleDocumentResponse create(long vehicleId, VehicleDocumentRequest req);

  List<VehicleDocumentResponse> list(long vehicleId);

  List<VehicleDocumentResponse> listMine(long vehicleId);
}
