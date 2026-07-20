package com.routeshare.routing.service;

import com.routeshare.routing.dto.response.RouteGeometryResponse;

public interface RouteGeometryService {
  /**
   * Returns the stored route-line segment between two route fractions for a published route
   * occurrence. This is the driver's actual published route, so the passenger map can render it
   * without any billable directions call.
   */
  RouteGeometryResponse occurrenceSegment(
      long routeOccurrenceId, double pickupFraction, double dropoffFraction);
}
