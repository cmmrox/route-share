package com.routeshare.trip.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Arrival, decided on explicit samples. Nothing here sleeps and nothing here reads a clock: the
 * trail carries its own timestamps, which is also what makes a disputed no-show re-examinable
 * months later.
 */
class ArrivalDetectorTest {
  private static final Instant T0 = Instant.parse("2026-08-02T09:20:00Z");
  private static final double GEOFENCE_METERS = 120;
  private static final Duration DWELL = Duration.ofSeconds(30);

  private final ArrivalDetector detector = new ArrivalDetector(GEOFENCE_METERS, DWELL);

  /** Samples every 10s, each at the given distance from the pickup point. */
  private List<ArrivalDetector.Sample> trail(double... distances) {
    List<ArrivalDetector.Sample> samples = new ArrayList<>();
    for (int i = 0; i < distances.length; i++) {
      samples.add(new ArrivalDetector.Sample(i + 1L, T0.plusSeconds(i * 10L), distances[i]));
    }
    return samples;
  }

  /** 05-8: inside the fence for the whole dwell — she is told her driver is here. */
  @Test
  void stayingInsideTheGeofenceForTheDwellIsAnArrival() {
    var arrival = detector.detect(trail(400, 200, 30, 25, 20, 22));

    assertThat(arrival).isPresent();
    assertThat(arrival.get().arrivedAt()).isEqualTo(T0.plusSeconds(20));
    assertThat(arrival.get().triggeringSampleIds()).containsExactly(3L, 4L, 5L, 6L);
  }

  /**
   * 05-9: the case the dwell requirement exists for. A route that runs past her corner clips the
   * geofence for a few seconds; without the dwell that starts a wait, and the wait ends in a
   * no-show fee charged to somebody whose driver never stopped.
   */
  @Test
  void drivingThroughTheGeofenceIsNotAnArrival() {
    assertThat(detector.detect(trail(400, 200, 40, 200, 600))).isEmpty();
  }

  /** Inside, but only just — one sample short of the dwell is still not waiting. */
  @Test
  void insideTheFenceForLessThanTheDwellIsNotYetAnArrival() {
    assertThat(detector.detect(trail(400, 200, 30, 25))).isEmpty();
  }

  /**
   * Two separate passes must not add up. A driver who clips the corner twice on a loop has been
   * near her twice and stopped neither time.
   */
  @Test
  void dwellDoesNotAccumulateAcrossSeparatePasses() {
    assertThat(detector.detect(trail(30, 25, 400, 30, 28))).isEmpty();
  }

  /**
   * Arrival is dated from entering the fence, not from when the dwell completed. Dating it later
   * would hand the driver back the seconds the dwell requirement cost, shortening her wait.
   */
  @Test
  void arrivalIsDatedFromEnteringTheFenceNotFromWhenDwellCompleted() {
    var arrival = detector.detect(trail(500, 30, 28, 26, 24));

    assertThat(arrival).isPresent();
    assertThat(arrival.get().arrivedAt()).isEqualTo(T0.plusSeconds(10));
  }

  /** A sample exactly on the boundary counts as inside; the fence is a radius, not a fence post. */
  @Test
  void aSampleOnTheGeofenceBoundaryCountsAsInside() {
    var arrival =
        detector.detect(trail(500, GEOFENCE_METERS, GEOFENCE_METERS, GEOFENCE_METERS, 10));

    assertThat(arrival).isPresent();
    assertThat(arrival.get().arrivedAt()).isEqualTo(T0.plusSeconds(10));
  }

  @Test
  void noTrailIsNoArrival() {
    assertThat(detector.detect(List.of())).isEmpty();
  }

  @Test
  void aTrailEntirelyOutsideTheFenceIsNoArrival() {
    assertThat(detector.detect(trail(900, 800, 700, 650))).isEmpty();
  }
}
