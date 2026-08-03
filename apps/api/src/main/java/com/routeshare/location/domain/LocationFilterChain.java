package com.routeshare.location.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Pure filter chain. Persistence stores every observation, while only accepted fixes move progress.
 */
@Component
public class LocationFilterChain {
  private static final double EARTH_RADIUS_METERS = 6_371_000.0;

  private final double maximumAccuracyMeters;
  private final double maximumSpeedMps;
  private final double routeCorridorMeters;
  private final double reversalToleranceFraction;

  public LocationFilterChain(
      @Value("${routeshare.location.accuracy-max-meters:50}") double maximumAccuracyMeters,
      @Value("${routeshare.location.max-speed-mps:40}") double maximumSpeedMps,
      @Value("${routeshare.location.route-corridor-meters:80}") double routeCorridorMeters,
      @Value("${routeshare.location.reversal-tolerance-fraction:0.005}")
          double reversalToleranceFraction) {
    this.maximumAccuracyMeters = maximumAccuracyMeters;
    this.maximumSpeedMps = maximumSpeedMps;
    this.routeCorridorMeters = routeCorridorMeters;
    this.reversalToleranceFraction = reversalToleranceFraction;
  }

  public Result apply(
      ObservedLocation sample,
      RouteProjection projection,
      ProgressState previous,
      Instant serverNow) {
    if (sample.accuracyMeters() > maximumAccuracyMeters) {
      return Result.rejected(LocationRejectionReason.ACCURACY_TOO_LOW, projection, previous);
    }
    if (previous != null
        && previous.matchedAt() != null
        && previous.latitude() != null
        && previous.longitude() != null) {
      double seconds =
          Math.max(
              0.001,
              Duration.between(previous.matchedAt(), sample.capturedAt()).toMillis() / 1000d);
      if (sample.capturedAt().isAfter(previous.matchedAt())) {
        double implied =
            haversineMeters(
                    previous.latitude(),
                    previous.longitude(),
                    sample.latitude(),
                    sample.longitude())
                / seconds;
        if (implied > maximumSpeedMps) {
          return Result.rejected(LocationRejectionReason.IMPLAUSIBLE_SPEED, projection, previous);
        }
      }
    }
    if (projection.offsetMeters() > routeCorridorMeters) {
      return Result.rejected(LocationRejectionReason.OFF_ROUTE, projection, previous);
    }
    if (previous != null
        && projection.fraction() + reversalToleranceFraction < previous.routeFraction()) {
      boolean confirms =
          previous.reversalCandidateFraction() != null
              && Math.abs(previous.reversalCandidateFraction() - projection.fraction())
                  <= reversalToleranceFraction
              && previous.reversalCandidateCount() >= 1;
      if (!confirms) {
        return new Result(
            false,
            Optional.of(LocationRejectionReason.BACKWARD_PROGRESS),
            projection,
            previous.routeFraction(),
            projection.fraction(),
            1);
      }
      // Store the confirmed wrong-turn observation, but clamp the offerable progress monotonically.
      return new Result(true, Optional.empty(), projection, previous.routeFraction(), null, 0);
    }
    return new Result(true, Optional.empty(), projection, projection.fraction(), null, 0);
  }

  public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);
    return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  public record Result(
      boolean accepted,
      Optional<LocationRejectionReason> rejection,
      RouteProjection projection,
      double progressFraction,
      Double reversalCandidateFraction,
      int reversalCandidateCount) {
    static Result rejected(
        LocationRejectionReason reason, RouteProjection projection, ProgressState previous) {
      return new Result(
          false,
          Optional.of(reason),
          projection,
          previous == null ? 0 : previous.routeFraction(),
          previous == null ? null : previous.reversalCandidateFraction(),
          previous == null ? 0 : previous.reversalCandidateCount());
    }
  }
}
