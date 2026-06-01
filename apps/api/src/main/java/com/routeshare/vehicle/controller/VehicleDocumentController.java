package com.routeshare.vehicle.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.vehicle.dto.request.VehicleDocumentRequest;
import com.routeshare.vehicle.dto.response.VehicleDocumentResponse;
import com.routeshare.vehicle.service.VehicleDocumentService;
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
@RequestMapping("/api/v1/driver/vehicles/{vehicleId}/documents")
@PreAuthorize("isAuthenticated()")
public class VehicleDocumentController {
  private final VehicleDocumentService service;

  public VehicleDocumentController(VehicleDocumentService service) {
    this.service = service;
  }

  @PostMapping
  ApiResponse<VehicleDocumentResponse> create(
      @PathVariable long vehicleId, @Valid @RequestBody VehicleDocumentRequest req) {
    return ApiResponse.ok(service.create(vehicleId, req));
  }

  @GetMapping
  ApiResponse<List<VehicleDocumentResponse>> list(@PathVariable long vehicleId) {
    return ApiResponse.ok(service.listMine(vehicleId));
  }
}
