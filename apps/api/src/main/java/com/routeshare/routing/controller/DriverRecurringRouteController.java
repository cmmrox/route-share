package com.routeshare.routing.controller;

import com.routeshare.common.security.DriverAccess;
import com.routeshare.common.security.DriverPublishAccess;
import com.routeshare.common.web.ApiResponse;
import com.routeshare.routing.dto.request.GenerateOccurrencesRequest;
import com.routeshare.routing.dto.request.RecurringRoutePublishRequest;
import com.routeshare.routing.dto.request.UpdateRecurringStatusRequest;
import com.routeshare.routing.dto.response.RecurringRouteResponse;
import com.routeshare.routing.service.RouteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver/recurring-routes")
@DriverAccess
public class DriverRecurringRouteController {
  private final RouteService routes;

  public DriverRecurringRouteController(RouteService routes) {
    this.routes = routes;
  }

  @DriverPublishAccess
  @PostMapping
  ApiResponse<Map<String, Object>> create(@Valid @RequestBody RecurringRoutePublishRequest req) {
    return ApiResponse.ok(routes.publishRecurring(req));
  }

  @GetMapping
  ApiResponse<List<RecurringRouteResponse>> list() {
    return ApiResponse.ok(routes.listRecurringRoutes());
  }

  @PutMapping("/{ruleId}")
  ApiResponse<RecurringRouteResponse> updateStatus(
      @PathVariable long ruleId, @Valid @RequestBody UpdateRecurringStatusRequest req) {
    return ApiResponse.ok(routes.updateRecurringStatus(ruleId, req.status()));
  }

  @DeleteMapping("/{ruleId}")
  ApiResponse<RecurringRouteResponse> cancel(@PathVariable long ruleId) {
    return ApiResponse.ok(routes.updateRecurringStatus(ruleId, "CANCELLED"));
  }

  @DriverPublishAccess
  @PostMapping("/{ruleId}/generate-occurrences")
  ApiResponse<Map<String, Object>> generate(
      @PathVariable long ruleId, @RequestBody(required = false) GenerateOccurrencesRequest req) {
    return ApiResponse.ok(
        routes.generateRecurringOccurrences(ruleId, req == null ? null : req.horizonDays()));
  }
}
