package com.routeshare.passenger.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.passenger.dto.request.*;
import com.routeshare.passenger.dto.response.*;
import com.routeshare.passenger.service.PassengerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/passenger/profile")
@PreAuthorize("isAuthenticated()")
public class PassengerProfileController {
  private final PassengerProfileService service;

  public PassengerProfileController(PassengerProfileService service) {
    this.service = service;
  }

  @GetMapping
  ApiResponse<PassengerProfileResponse> get() {
    return ApiResponse.ok(
        service
            .get()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Passenger profile not found")));
  }

  @PutMapping
  ApiResponse<PassengerProfileResponse> upsert(@Valid @RequestBody PassengerProfileRequest req) {
    return ApiResponse.ok(service.upsert(req));
  }
}
