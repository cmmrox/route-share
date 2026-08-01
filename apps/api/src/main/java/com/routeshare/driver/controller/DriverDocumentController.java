package com.routeshare.driver.controller;

import com.routeshare.common.security.DriverAccess;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.driver.dto.response.DriverDocumentResponse;
import com.routeshare.driver.service.DriverDocumentService;
import com.routeshare.storage.dto.DownloadUrlResponse;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.dto.UploadUrlResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver/documents")
@DriverAccess
public class DriverDocumentController {
  private final DriverDocumentService service;

  public DriverDocumentController(DriverDocumentService service) {
    this.service = service;
  }

  @PostMapping("/upload-url")
  ApiResponse<UploadUrlResponse> createUploadUrl(@Valid @RequestBody UploadUrlRequest req) {
    return ApiResponse.ok(service.createUploadUrl(req));
  }

  @PostMapping("/{documentId}/submit")
  ApiResponse<DriverDocumentResponse> submit(@PathVariable long documentId) {
    return ApiResponse.ok(service.submit(documentId));
  }

  @GetMapping
  ApiResponse<List<DriverDocumentResponse>> list() {
    return ApiResponse.ok(service.listMine());
  }

  @GetMapping("/{documentId}/download-url")
  ApiResponse<DownloadUrlResponse> downloadUrl(@PathVariable long documentId) {
    return ApiResponse.ok(service.downloadUrl(documentId));
  }
}
