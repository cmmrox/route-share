package com.routeshare.vehicle.controller;

import com.routeshare.common.security.DriverAccess;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.storage.dto.DownloadUrlResponse;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.dto.UploadUrlResponse;
import com.routeshare.vehicle.dto.response.VehicleDocumentResponse;
import com.routeshare.vehicle.service.VehicleDocumentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver/vehicles/{vehicleId}/documents")
@DriverAccess
public class VehicleDocumentController {
  private final VehicleDocumentService service;

  public VehicleDocumentController(VehicleDocumentService service) {
    this.service = service;
  }

  @PostMapping("/upload-url")
  ApiResponse<UploadUrlResponse> createUploadUrl(
      @PathVariable long vehicleId, @Valid @RequestBody UploadUrlRequest req) {
    return ApiResponse.ok(service.createUploadUrl(vehicleId, req));
  }

  @PostMapping("/{documentId}/submit")
  ApiResponse<VehicleDocumentResponse> submit(
      @PathVariable long vehicleId, @PathVariable long documentId) {
    return ApiResponse.ok(service.submit(vehicleId, documentId));
  }

  @GetMapping
  ApiResponse<List<VehicleDocumentResponse>> list(@PathVariable long vehicleId) {
    return ApiResponse.ok(service.listMine(vehicleId));
  }

  @GetMapping("/{documentId}/download-url")
  ApiResponse<DownloadUrlResponse> downloadUrl(
      @PathVariable long vehicleId, @PathVariable long documentId) {
    return ApiResponse.ok(service.downloadUrl(vehicleId, documentId));
  }
}
