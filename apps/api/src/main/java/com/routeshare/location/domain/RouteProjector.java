package com.routeshare.location.domain;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/** Resolves loop candidates without allowing a noisy crossing to move progress backwards. */
@Component
public class RouteProjector {
  public RouteProjection selectCandidate(
      List<RouteProjection> candidates, Double previousFraction) {
    if (candidates == null || candidates.isEmpty()) {
      throw new IllegalArgumentException("At least one route projection is required");
    }
    if (previousFraction == null) {
      return candidates.stream()
          .min(Comparator.comparingDouble(RouteProjection::offsetMeters))
          .orElseThrow();
    }
    double nearestOffset =
        candidates.stream().mapToDouble(RouteProjection::offsetMeters).min().orElseThrow();
    return candidates.stream()
        .filter(candidate -> candidate.offsetMeters() <= nearestOffset + 10)
        .min(
            Comparator.comparingDouble(
                    (RouteProjection p) ->
                        p.fraction() + 0.005 < previousFraction
                            ? 1_000_000 + (previousFraction - p.fraction())
                            : Math.abs(p.fraction() - previousFraction))
                .thenComparingDouble(RouteProjection::offsetMeters))
        .orElseThrow();
  }
}
