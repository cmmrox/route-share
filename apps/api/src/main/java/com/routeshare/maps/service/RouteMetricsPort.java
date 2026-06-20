package com.routeshare.maps.service;

/**
 * Server-side distance + duration between two points, used so fares are computed from authoritative
 * routing data rather than a client-supplied distance. Backed by Google Distance Matrix when
 * configured, otherwise a great-circle estimate.
 */
public interface RouteMetricsPort {
  record RouteMetrics(long distanceMeters, long durationSeconds, String source) {}

  RouteMetrics distanceAndDuration(
      double originLat, double originLng, double destLat, double destLng);
}
