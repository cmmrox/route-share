package com.routeshare.routing.domain;

import org.springframework.stereotype.Component;

@Component
public class RouteMatchScorer {
  private static final double OVERLAP_WEIGHT = 0.60;
  private static final double PICKUP_WEIGHT = 0.20;
  private static final double DROPOFF_WEIGHT = 0.20;
  private static final double DEFAULT_PROXIMITY_RADIUS_METERS = 1_000.0;

  public RouteMatchScore score(RouteMatchCandidate candidate) {
    if (candidate.pickupFraction() >= candidate.dropoffFraction()) {
      throw new IllegalArgumentException(
          "Route match pickup must be before drop-off on driver route");
    }
    double requestedDistance = Math.max(candidate.requestedDistanceMeters(), 1.0);
    double overlapPercent =
        clamp(candidate.overlapDistanceMeters() / requestedDistance, 0, 1) * 100.0;
    double overlapScore = overlapPercent / 100.0;
    double pickupScore = proximityScore(candidate.pickupDistanceMeters());
    double dropoffScore = proximityScore(candidate.dropoffDistanceMeters());
    double weightedScore =
        (OVERLAP_WEIGHT * overlapScore
                + PICKUP_WEIGHT * pickupScore
                + DROPOFF_WEIGHT * dropoffScore)
            * 100.0;
    return new RouteMatchScore(
        round(weightedScore),
        round(overlapPercent),
        round(pickupScore * 100.0),
        round(dropoffScore * 100.0),
        "Route ranked by overlap, pickup proximity, and drop-off proximity");
  }

  private double proximityScore(double distanceMeters) {
    return 1.0 - clamp(distanceMeters / DEFAULT_PROXIMITY_RADIUS_METERS, 0, 1);
  }

  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  private double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
