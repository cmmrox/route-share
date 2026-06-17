package com.routeshare.vehicle.service;

import com.routeshare.storage.dto.DownloadUrlResponse;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.dto.UploadUrlResponse;
import com.routeshare.vehicle.dto.response.VehicleDocumentResponse;
import java.util.List;

public interface VehicleDocumentService {
  UploadUrlResponse createUploadUrl(long vehicleId, UploadUrlRequest req);

  VehicleDocumentResponse submit(long vehicleId, long documentId);

  List<VehicleDocumentResponse> listMine(long vehicleId);

  DownloadUrlResponse downloadUrl(long vehicleId, long documentId);
}
