package com.routeshare.routing.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.routing.dto.request.RoutePublishRequest;
import com.routeshare.routing.dto.response.DriverRouteResponse;
import com.routeshare.routing.service.RouteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver/routes")
@PreAuthorize("hasAnyRole('DRIVER','ADMIN','SUPER_ADMIN')")
public class DriverRouteController {
  private final RouteService routes;

  public DriverRouteController(RouteService routes) {
    this.routes = routes;
  }

  @GetMapping
  public ApiResponse<List<DriverRouteResponse>> list() {
    return ApiResponse.ok(routes.listDriverRoutes());
  }

  @GetMapping("/{routeId}")
  public ApiResponse<DriverRouteResponse> get(@PathVariable long routeId) {
    return ApiResponse.ok(routes.getDriverRoute(routeId));
  }

  @PostMapping("/{routeId}/share-link")
  public ApiResponse<Map<String, Object>> shareLink(@PathVariable long routeId) {
    return ApiResponse.ok(routes.createShareLink(routeId));
  }

  @PostMapping("/{routeId}/cancel")
  public ApiResponse<Map<String, Object>> cancel(@PathVariable long routeId) {
    return ApiResponse.ok(routes.cancelDriverRoute(routeId));
  }

  @PostMapping
  public ApiResponse<Map<String, Object>> create(@Valid @RequestBody RoutePublishRequest req) {
    return ApiResponse.ok(routes.publish(req));
  }
}
