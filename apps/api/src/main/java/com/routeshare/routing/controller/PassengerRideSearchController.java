package com.routeshare.routing.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.routing.dto.request.RouteSearchRequest;
import com.routeshare.routing.dto.response.RouteSearchResponse;
import com.routeshare.routing.service.RouteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/passenger/ride-searches")
@PreAuthorize("isAuthenticated()")
public class PassengerRideSearchController {
  private final RouteService routes;

  public PassengerRideSearchController(RouteService routes) {
    this.routes = routes;
  }

  @PostMapping
  ApiResponse<List<RouteSearchResponse>> create(@Valid @RequestBody RouteSearchRequest req) {
    return ApiResponse.ok(routes.search(req));
  }
}
