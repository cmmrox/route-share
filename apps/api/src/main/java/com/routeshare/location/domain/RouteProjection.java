package com.routeshare.location.domain;

public record RouteProjection(double fraction, double offsetMeters, double remainingMeters) {
  public RouteProjection {
    fraction = Math.max(0.0, Math.min(1.0, fraction));
    offsetMeters = Math.max(0.0, offsetMeters);
    remainingMeters = Math.max(0.0, remainingMeters);
  }
}
