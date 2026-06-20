package com.routeshare.admin.controller;

import com.routeshare.appreadiness.service.AppReadinessService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Residual admin endpoints not yet migrated to real domains. As of Phase 06.6-G4 only two remain on
 * the workflow_item facade:
 *
 * <ul>
 *   <li>driver-application review — pending a dedicated driver-verification workflow,
 *   <li>trip location-trail — pending PostGIS coordinate extraction (lands with Phase H maps).
 * </ul>
 *
 * Everything else (dashboard, users, audit, support, SOS, finance, document review, broadcasts,
 * trips/bookings/verification read projections, reports) is served by the real Admin* controllers.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','OPS_ADMIN','VERIFICATION_AGENT')")
public class AdminAppReadinessController {
  private final AppReadinessService service;

  @PostMapping("/api/v1/admin/driver-applications/{driverId}/review")
  public Map<String, Object> reviewDriverApplication(
      @PathVariable long driverId, @RequestBody(required = false) Map<String, Object> body) {
    return service.create(
        "DRIVER_APPLICATION_REVIEW", "ADMIN", "DRIVER", String.valueOf(driverId), body);
  }
}
