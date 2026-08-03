package com.routeshare.location.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Geometry divided by observed speed. This class deliberately has no maps/provider dependency. */
@Component
public class EtaCalculator {
  private final double corridorFallbackMetersPerSecond;

  public EtaCalculator(
      @Value("${routeshare.location.corridor-fallback-speed-kmh:22}")
          double corridorFallbackSpeedKmh) {
    this.corridorFallbackMetersPerSecond = corridorFallbackSpeedKmh / 3.6;
  }

  public long etaSeconds(double remainingMeters, Double observedSpeedMps) {
    double speed =
        observedSpeedMps != null && observedSpeedMps > 0.5
            ? observedSpeedMps
            : corridorFallbackMetersPerSecond;
    return Math.max(0, Math.round(Math.max(0, remainingMeters) / speed));
  }

  public double smooth(Double previousSpeedMps, Double observedSpeedMps) {
    if (observedSpeedMps == null) {
      return previousSpeedMps == null ? corridorFallbackMetersPerSecond : previousSpeedMps;
    }
    if (previousSpeedMps == null) {
      return observedSpeedMps;
    }
    return (previousSpeedMps * 0.8) + (observedSpeedMps * 0.2);
  }
}
