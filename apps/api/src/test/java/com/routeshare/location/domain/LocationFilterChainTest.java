package com.routeshare.location.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class LocationFilterChainTest {
  private final LocationFilterChain filters = new LocationFilterChain(50, 40, 80, 0.005);
  private final Instant now = Instant.parse("2026-08-04T00:00:00Z");

  @Test
  void appliesAccuracySpeedRouteAndMonotonicFiltersInOrder() {
    var poor = sample("poor", now, 6.9, 79.8, 80);
    assertThat(filters.apply(poor, projection(0.2, 10), null, now).rejection())
        .contains(LocationRejectionReason.ACCURACY_TOO_LOW);

    var previous = progress(0.2, now.minusSeconds(1), 6.9, 79.8, null, 0);
    var jump = sample("jump", now, 7.0, 80.0, 10);
    assertThat(filters.apply(jump, projection(0.3, 10), previous, now).rejection())
        .contains(LocationRejectionReason.IMPLAUSIBLE_SPEED);

    assertThat(
            filters
                .apply(sample("off", now, 6.9, 79.8, 10), projection(0.3, 90), null, now)
                .rejection())
        .contains(LocationRejectionReason.OFF_ROUTE);

    assertThat(
            filters
                .apply(
                    sample("back", now, 6.9, 79.8, 10),
                    projection(0.1, 10),
                    progress(0.2, now.minusSeconds(5), 6.9, 79.8, null, 0),
                    now)
                .rejection())
        .contains(LocationRejectionReason.BACKWARD_PROGRESS);
  }

  @Test
  void secondMatchingReversalIsStoredWithoutMovingOfferableProgressBackward() {
    var previous = progress(0.4, now.minusSeconds(5), 6.9, 79.8, 0.3, 1);
    var result =
        filters.apply(
            sample("confirmed", now, 6.90001, 79.80001, 10), projection(0.301, 10), previous, now);
    assertThat(result.accepted()).isTrue();
    assertThat(result.progressFraction()).isEqualTo(0.4);
  }

  private ObservedLocation sample(
      String id, Instant capturedAt, double lat, double lng, double accuracy) {
    return new ObservedLocation(id, capturedAt, lat, lng, accuracy, 8.0, 90.0, 80);
  }

  private RouteProjection projection(double fraction, double offset) {
    return new RouteProjection(fraction, offset, 1_000);
  }

  private ProgressState progress(
      double fraction,
      Instant matchedAt,
      double lat,
      double lng,
      Double candidate,
      int candidateCount) {
    return new ProgressState(
        fraction,
        LocationConfidence.MATCHED,
        matchedAt,
        matchedAt,
        8.0,
        90.0,
        lat,
        lng,
        null,
        candidate,
        candidateCount);
  }
}
