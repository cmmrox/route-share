package com.routeshare.passenger.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.passenger.dto.response.PassengerDocumentResponse;
import com.routeshare.passenger.service.PassengerDocumentService;
import com.routeshare.storage.dto.DownloadUrlResponse;
import com.routeshare.storage.dto.UploadUrlRequest;
import com.routeshare.storage.dto.UploadUrlResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passenger/documents")
@PreAuthorize("hasRole('PASSENGER')")
public class PassengerDocumentController {
  private final PassengerDocumentService service;

  public PassengerDocumentController(PassengerDocumentService service) {
    this.service = service;
  }

  @PostMapping("/upload-url")
  ApiResponse<UploadUrlResponse> createUploadUrl(@Valid @RequestBody UploadUrlRequest req) {
    return ApiResponse.ok(service.createUploadUrl(req));
  }

  @PostMapping("/{documentId}/submit")
  ApiResponse<PassengerDocumentResponse> submit(@PathVariable long documentId) {
    return ApiResponse.ok(service.submit(documentId));
  }

  @GetMapping
  ApiResponse<List<PassengerDocumentResponse>> list() {
    return ApiResponse.ok(service.listMine());
  }

  @GetMapping("/{documentId}/download-url")
  ApiResponse<DownloadUrlResponse> downloadUrl(@PathVariable long documentId) {
    return ApiResponse.ok(service.downloadUrl(documentId));
  }
}
