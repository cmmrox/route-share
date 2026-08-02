package com.routeshare.passenger.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.passenger.dto.request.UsualCommuteRequest;
import com.routeshare.passenger.dto.response.UsualCommuteResponse;
import com.routeshare.passenger.service.UsualCommuteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** P02 — the commuter dashboard. */
@RestController
@RequestMapping("/api/v1/passenger/commute")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class UsualCommuteController {

  private final UsualCommuteService commutes;

  @GetMapping
  ApiResponse<UsualCommuteResponse> mine() {
    return ApiResponse.ok(commutes.mine());
  }

  @PutMapping
  ApiResponse<UsualCommuteResponse> save(@Valid @RequestBody UsualCommuteRequest request) {
    return ApiResponse.ok(commutes.save(request));
  }

  @DeleteMapping
  ApiResponse<UsualCommuteResponse> clear() {
    commutes.clear();
    return ApiResponse.ok(UsualCommuteResponse.none());
  }
}
