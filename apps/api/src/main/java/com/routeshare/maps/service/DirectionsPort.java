package com.routeshare.maps.service;

import com.routeshare.maps.dto.CoordinateResponse;
import java.util.List;

/**
 * Road-following driving route between two points. Backed by the Google Directions API when maps
 * are configured; otherwise degrades to a straight two-point line so the UI still has something to
 * draw.
 */
public interface DirectionsPort {
  record DirectionsResult(
      List<CoordinateResponse> coordinates,
      long distanceMeters,
      long durationSeconds,
      String source) {}

  DirectionsResult route(double originLat, double originLng, double destLat, double destLng);
}
