package com.routeshare.routing.controller;

import com.routeshare.common.web.ApiResponse;
import com.routeshare.routing.dto.request.RoutePublishRequest;
import com.routeshare.routing.dto.request.RouteSearchRequest;
import com.routeshare.routing.dto.response.RouteSearchResponse;
import com.routeshare.routing.service.RouteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {
  private final RouteService routes;

  public RouteController(RouteService routes) {
    this.routes = routes;
  }

  // Publishing is the one driver action with its own gate: approved documents, an approved
  // vehicle and (slice 02) a rate band. Admins keep their operational override.
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN') or @driverGuard.canPublish(authentication)")
  ApiResponse<Map<String, Object>> publish(@Valid @RequestBody RoutePublishRequest req) {
    return ApiResponse.ok(routes.publish(req));
  }

  @PostMapping("/search")
  @PreAuthorize("isAuthenticated()")
  ApiResponse<List<RouteSearchResponse>> search(@Valid @RequestBody RouteSearchRequest req) {
    return ApiResponse.ok(routes.search(req));
  }
}
