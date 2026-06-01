package com.routeshare.driver.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.driver.dto.request.DocumentMetadataRequest;
import com.routeshare.driver.dto.response.DriverDocumentResponse;
import com.routeshare.driver.service.DriverDocumentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver/documents")
@PreAuthorize("isAuthenticated()")
public class DriverDocumentController {
  private final DriverDocumentService service;

  public DriverDocumentController(DriverDocumentService service) {
    this.service = service;
  }

  @PostMapping
  ApiResponse<DriverDocumentResponse> create(@Valid @RequestBody DocumentMetadataRequest req) {
    return ApiResponse.ok(service.create(req));
  }

  @GetMapping
  ApiResponse<List<DriverDocumentResponse>> list() {
    return ApiResponse.ok(service.listMine());
  }
}
