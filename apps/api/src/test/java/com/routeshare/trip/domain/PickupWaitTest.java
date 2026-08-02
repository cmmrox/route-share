package com.routeshare.trip.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.routeshare.trip.entity.PickupWaitEntity;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** The pickup-wait arithmetic, on explicit instants. Nothing here sleeps. */
class PickupWaitTest {
  private static final Instant ARRIVED = Instant.parse("2026-08-02T09:20:00Z");
  private static final Duration WAIT = Duration.ofMinutes(5);
  private static final Duration EXTENSION = Duration.ofMinutes(5);
  private static final int LIMIT = 1;

  private PickupWaitEntity newWait() {
    return PickupWaitEntity.startedOnArrival(77L, 100L, ARRIVED, WAIT, "{}");
  }

  /**
   * The wait runs from the detected arrival, not from when the row was written. A slow write must
   * not shorten her five minutes and a delayed one must not lengthen them.
   */
  @Test
  void theWaitRunsFromTheDetectedArrival() {
    assertThat(newWait().effectiveDeadline()).isEqualTo(ARRIVED.plus(Duration.ofMinutes(5)));
  }

  /** 05-11: at +5 the extension is still there to spend. */
  @Test
  void theExtensionIsAvailableUntilItIsSpent() {
    var wait = newWait();

    assertThat(wait.hasExtensionRemaining(LIMIT)).isTrue();
    assertThat(wait.extend(EXTENSION, LIMIT)).isTrue();
    assertThat(wait.hasExtensionRemaining(LIMIT)).isFalse();
  }

  /**
   * 05-12: the extension moves the deadline to +10 from the arrival. Measured from the deadline,
   * not from the tap — extending at 4:59 must not buy nearly ten further minutes.
   */
  @Test
  void theExtensionIsMeasuredFromTheDeadlineNotFromWhenItWasTapped() {
    var wait = newWait();
    wait.extend(EXTENSION, LIMIT);

    assertThat(wait.effectiveDeadline()).isEqualTo(ARRIVED.plus(Duration.ofMinutes(10)));
  }

  /** 05-6's shape on this clock: the second attempt is refused as data, not by throwing. */
  @Test
  void theSecondExtensionIsRefused() {
    var wait = newWait();
    assertThat(wait.extend(EXTENSION, LIMIT)).isTrue();

    assertThat(wait.extend(EXTENSION, LIMIT)).isFalse();
    assertThat(wait.effectiveDeadline()).isEqualTo(ARRIVED.plus(Duration.ofMinutes(10)));
  }

  /** A wait that has ended cannot be extended back into life. */
  @Test
  void aResolvedWaitCannotBeExtended() {
    var wait = newWait();
    wait.resolve(PickupWaitEntity.RESOLUTION_BOARDED, ARRIVED.plusSeconds(60));

    assertThat(wait.extend(EXTENSION, LIMIT)).isFalse();
  }

  /** Resolution is idempotent: a repeated sweep keeps the first outcome and its timestamp. */
  @Test
  void resolutionKeepsTheFirstOutcome() {
    var wait = newWait();
    Instant first = ARRIVED.plus(Duration.ofMinutes(5));
    wait.resolve(PickupWaitEntity.RESOLUTION_NO_SHOW, first);
    wait.resolve(PickupWaitEntity.RESOLUTION_BOARDED, first.plusSeconds(30));

    assertThat(wait.getResolution()).isEqualTo(PickupWaitEntity.RESOLUTION_NO_SHOW);
    assertThat(wait.getResolvedAt()).isEqualTo(first);
  }

  /** The evidence travels with the row; a no-show with no trail is one support cannot defend. */
  @Test
  void theTriggeringSamplesAreKeptOnTheRow() {
    var wait =
        PickupWaitEntity.startedOnArrival(
            77L, 100L, ARRIVED, WAIT, "{\"locationSampleIds\":[3,4,5]}");

    assertThat(wait.getTriggeredBySamples()).contains("locationSampleIds");
  }
}
