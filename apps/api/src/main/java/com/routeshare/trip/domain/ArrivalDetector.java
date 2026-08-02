package com.routeshare.trip.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Decides whether a driver has arrived at a pickup point, from the location trail alone.
 *
 * <p>Geofence <em>and</em> dwell, because a geofence on its own is not arrival. Any route that
 * passes within a hundred metres of a corner would trigger one, and the wait it starts ends in a
 * no-show that costs the passenger a fee and a mark on her record — manufactured by a driver who
 * simply drove past on the way to somewhere else.
 *
 * <p>Pure on purpose: this is the rule that has to be re-examined when a no-show is disputed, and
 * it should be readable and testable without a database or a clock.
 */
public final class ArrivalDetector {

  private final double geofenceMeters;
  private final Duration dwell;

  public ArrivalDetector(double geofenceMeters, Duration dwell) {
    this.geofenceMeters = geofenceMeters;
    this.dwell = dwell;
  }

  /** One location sample, reduced to what the decision actually depends on. */
  public record Sample(long sampleId, Instant recordedAt, double distanceMeters) {}

  /** An arrival, with the samples that established it so the row can carry its own evidence. */
  public record Arrival(Instant arrivedAt, List<Long> triggeringSampleIds) {}

  /**
   * @param trail samples for this trip against one pickup point, oldest first. Callers pass the
   *     window they consider recent; anything older cannot establish a present arrival.
   */
  public Optional<Arrival> detect(List<Sample> trail) {
    if (trail.isEmpty()) {
      return Optional.empty();
    }

    // Walk back from the newest sample for as long as the driver has stayed inside the fence. The
    // moment a sample outside it appears, the run ends there — that is precisely what a drive-past
    // looks like, and why one cannot accumulate dwell across two separate passes.
    int firstOfRun = trail.size();
    for (int i = trail.size() - 1; i >= 0; i--) {
      if (trail.get(i).distanceMeters() > geofenceMeters) {
        break;
      }
      firstOfRun = i;
    }
    if (firstOfRun == trail.size()) {
      return Optional.empty();
    }

    List<Sample> inside = trail.subList(firstOfRun, trail.size());
    Instant enteredAt = inside.getFirst().recordedAt();
    Instant latestAt = inside.getLast().recordedAt();
    if (Duration.between(enteredAt, latestAt).compareTo(dwell) < 0) {
      // Inside the fence, but not yet for long enough to be waiting rather than passing.
      return Optional.empty();
    }

    // Arrival is dated from when the driver reached the pickup, not from when the dwell completed.
    // Dating it later would quietly hand the driver back the seconds the dwell requirement cost.
    return Optional.of(new Arrival(enteredAt, inside.stream().map(Sample::sampleId).toList()));
  }
}
