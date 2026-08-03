package com.routeshare.safety.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.safety.dto.*;
import com.routeshare.safety.service.SosService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sos-events")
@PreAuthorize("isAuthenticated()")
public class UnifiedSosController {
  private final SosService sos;

  public UnifiedSosController(SosService sos) {
    this.sos = sos;
  }

  @PostMapping
  ApiResponse<SosEventResponse> raise(@Valid @RequestBody RaiseSosRequest request) {
    return ApiResponse.ok(sos.raiseCurrent(request));
  }

  @GetMapping("/{id}")
  ApiResponse<SosEventResponse> get(@PathVariable long id) {
    return ApiResponse.ok(sos.getMine(id));
  }
}
