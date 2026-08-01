package com.routeshare.safety.controller;

import com.routeshare.common.security.DriverAccess;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.safety.dto.RaiseSosRequest;
import com.routeshare.safety.dto.SosEventResponse;
import com.routeshare.safety.service.SosService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver/sos-events")
@DriverAccess
public class DriverSosController {
  private final SosService service;

  public DriverSosController(SosService service) {
    this.service = service;
  }

  @PostMapping
  ApiResponse<SosEventResponse> raise(@Valid @RequestBody RaiseSosRequest req) {
    return ApiResponse.ok(service.raise("DRIVER", req));
  }

  @GetMapping
  ApiResponse<List<SosEventResponse>> list() {
    return ApiResponse.ok(service.listMine());
  }
}
