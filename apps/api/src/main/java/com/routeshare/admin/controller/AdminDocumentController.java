package com.routeshare.admin.controller;

import com.routeshare.admin.dto.AdminDocReviewRequest;
import com.routeshare.admin.dto.AdminDocumentResponse;
import com.routeshare.admin.service.AdminDocumentService;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.storage.dto.DownloadUrlResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','VERIFICATION_AGENT','OPS_ADMIN')")
public class AdminDocumentController {
  private final AdminDocumentService service;

  public AdminDocumentController(AdminDocumentService service) {
    this.service = service;
  }

  @PostMapping("/api/v1/admin/driver-documents/{documentId}/review")
  ApiResponse<AdminDocumentResponse> reviewDriverDocument(
      @PathVariable long documentId, @Valid @RequestBody AdminDocReviewRequest req) {
    return ApiResponse.ok(service.reviewDriverDocument(documentId, req));
  }

  @PostMapping("/api/v1/admin/vehicle-documents/{documentId}/review")
  ApiResponse<AdminDocumentResponse> reviewVehicleDocument(
      @PathVariable long documentId, @Valid @RequestBody AdminDocReviewRequest req) {
    return ApiResponse.ok(service.reviewVehicleDocument(documentId, req));
  }

  @PostMapping("/api/v1/admin/passenger-documents/{documentId}/review")
  ApiResponse<AdminDocumentResponse> reviewPassengerDocument(
      @PathVariable long documentId, @Valid @RequestBody AdminDocReviewRequest req) {
    return ApiResponse.ok(service.reviewPassengerDocument(documentId, req));
  }

  @GetMapping("/api/v1/admin/driver-documents/{documentId}/download-url")
  ApiResponse<DownloadUrlResponse> driverDocumentDownloadUrl(@PathVariable long documentId) {
    return ApiResponse.ok(service.driverDocumentDownloadUrl(documentId));
  }

  @GetMapping("/api/v1/admin/vehicle-documents/{documentId}/download-url")
  ApiResponse<DownloadUrlResponse> vehicleDocumentDownloadUrl(@PathVariable long documentId) {
    return ApiResponse.ok(service.vehicleDocumentDownloadUrl(documentId));
  }

  @GetMapping("/api/v1/admin/passenger-documents/{documentId}/download-url")
  ApiResponse<DownloadUrlResponse> passengerDocumentDownloadUrl(@PathVariable long documentId) {
    return ApiResponse.ok(service.passengerDocumentDownloadUrl(documentId));
  }
}
