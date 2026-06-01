package com.routeshare.vehicle.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.vehicle.dto.request.*;
import com.routeshare.vehicle.dto.response.*;
import com.routeshare.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/driver/vehicles")
@PreAuthorize("isAuthenticated()")
public class VehicleController {
  private final VehicleService service;

  public VehicleController(VehicleService service) {
    this.service = service;
  }

  @PostMapping
  ApiResponse<VehicleResponse> create(@Valid @RequestBody VehicleRequest req) {
    return ApiResponse.ok(service.create(req));
  }

  @GetMapping
  ApiResponse<List<VehicleResponse>> list() {
    return ApiResponse.ok(service.listMine());
  }
}
