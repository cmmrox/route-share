package com.routeshare.location.domain;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeadReckoner {
  private final long extrapolationMaximumSeconds;

  public DeadReckoner(
      @Value("${routeshare.location.extrapolation-max-seconds:20}")
          long extrapolationMaximumSeconds) {
    this.extrapolationMaximumSeconds = extrapolationMaximumSeconds;
  }

  public Estimate estimate(ProgressState progress, double routeLengthMeters, Instant now) {
    long ageSeconds = Math.max(0, Duration.between(progress.matchedAt(), now).toSeconds());
    if (progress.confidence() == LocationConfidence.OFF_ROUTE) {
      return new Estimate(progress.routeFraction(), LocationConfidence.OFF_ROUTE, ageSeconds);
    }
    if (ageSeconds == 0) {
      return new Estimate(progress.routeFraction(), LocationConfidence.MATCHED, 0);
    }
    if (ageSeconds > extrapolationMaximumSeconds || progress.speedMps() == null) {
      return new Estimate(progress.routeFraction(), LocationConfidence.STALE, ageSeconds);
    }
    double advanced =
        routeLengthMeters <= 0
            ? 0
            : (Math.max(0, progress.speedMps()) * ageSeconds) / routeLengthMeters;
    return new Estimate(
        Math.min(1.0, progress.routeFraction() + advanced),
        LocationConfidence.EXTRAPOLATED,
        ageSeconds);
  }

  public record Estimate(double routeFraction, LocationConfidence confidence, long ageSeconds) {}
}
