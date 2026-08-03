package com.routeshare.location;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.location.domain.*;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class TraceReplayIT {
  private final ObjectMapper json = new ObjectMapper();
  private final LocationFilterChain filters = new LocationFilterChain(50, 40, 80, 0.005);
  private final Instant base = Instant.parse("2026-08-04T00:00:00Z");

  @Test
  void clearAndUrbanCanyonTracesNeverMoveAcceptedProgressBackward() throws Exception {
    assertMonotonic("trace-clear-sky.json");
    assertMonotonic("trace-urban-canyon.json");
  }

  @Test
  void spikeDetourLoopAndTunnelExerciseEveryDegradedState() throws Exception {
    List<Fix> jump = load("trace-jump.json");
    ProgressState first = state(jump.getFirst(), null, 0);
    assertThat(apply(jump.get(1), first).rejection())
        .contains(LocationRejectionReason.IMPLAUSIBLE_SPEED);

    List<Fix> detour = load("trace-detour.json");
    assertThat(apply(detour.get(1), state(detour.getFirst(), null, 0)).rejection())
        .contains(LocationRejectionReason.OFF_ROUTE);

    List<Fix> loop = load("trace-loop.json");
    var loopResult = apply(loop.get(1), state(loop.getFirst(), null, 0));
    assertThat(loopResult.rejection()).contains(LocationRejectionReason.BACKWARD_PROGRESS);

    var tunnelStart = state(load("trace-tunnel-gap.json").getFirst(), null, 0);
    assertThat(
            new DeadReckoner(20).estimate(tunnelStart, 10_000, base.plusSeconds(25)).confidence())
        .isEqualTo(LocationConfidence.STALE);
  }

  private void assertMonotonic(String fixture) throws Exception {
    ProgressState state = null;
    double last = 0;
    for (Fix fix : load(fixture)) {
      var result = apply(fix, state);
      if (result.accepted()) {
        assertThat(result.progressFraction()).isGreaterThanOrEqualTo(last);
        last = result.progressFraction();
        state = state(fix, null, 0, result.progressFraction());
      } else if (result.rejection().orElse(null) == LocationRejectionReason.BACKWARD_PROGRESS) {
        state = state(fix, fix.fraction(), 1, last);
      }
    }
  }

  private LocationFilterChain.Result apply(Fix fix, ProgressState previous) {
    return filters.apply(
        new ObservedLocation(
            fix.id(),
            base.plusSeconds(fix.seconds()),
            fix.lat(),
            fix.lng(),
            fix.accuracy(),
            8.0,
            90.0,
            80),
        new RouteProjection(fix.fraction(), fix.offset(), 1_000),
        previous,
        base.plusSeconds(fix.seconds()));
  }

  private ProgressState state(Fix fix, Double candidate, int candidateCount) {
    return state(fix, candidate, candidateCount, fix.fraction());
  }

  private ProgressState state(
      Fix fix, Double candidate, int candidateCount, double progressFraction) {
    Instant at = base.plusSeconds(fix.seconds());
    return new ProgressState(
        progressFraction,
        LocationConfidence.MATCHED,
        at,
        at,
        8.0,
        90.0,
        fix.lat(),
        fix.lng(),
        null,
        candidate,
        candidateCount);
  }

  private List<Fix> load(String name) throws Exception {
    try (InputStream input = getClass().getResourceAsStream("/location-traces/" + name)) {
      return json.readValue(input, new TypeReference<>() {});
    }
  }

  record Fix(
      String id,
      long seconds,
      double lat,
      double lng,
      double accuracy,
      double fraction,
      double offset) {}
}
