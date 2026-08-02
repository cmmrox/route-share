package com.routeshare.trip.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.routeshare.trip.entity.TripStartWindowEntity;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * The start-buffer arithmetic, driven by explicit instants. Nothing here sleeps: a timer test that
 * passes because it waited is a test that will fail on a slow machine and be re-run until green.
 */
class TripStartWindowTest {
  private static final Instant DEPARTS = Instant.parse("2026-08-02T09:00:00Z");
  private static final Duration BUFFER = Duration.ofMinutes(10);
  private static final Duration EXTENSION = Duration.ofMinutes(10);
  private static final int LIMIT = 1;

  @Test
  void theBufferRunsFromDepartureNotFromWhenTheRowWasWritten() {
    var window = TripStartWindowEntity.opening(1L, DEPARTS, BUFFER);

    assertThat(window.getBufferExpiresAt()).isEqualTo(DEPARTS.plusSeconds(600));
    assertThat(window.effectiveDeadline()).isEqualTo(DEPARTS.plusSeconds(600));
  }

  /** 05-3: departure + 9 min, not started — nothing yet. */
  @Test
  void nineMinutesInTheWindowHasNotExpired() {
    var window = TripStartWindowEntity.opening(1L, DEPARTS, BUFFER);

    assertThat(window.effectiveDeadline()).isAfter(DEPARTS.plus(Duration.ofMinutes(9)));
  }

  /** 05-4: departure + 11 min — past the deadline. */
  @Test
  void elevenMinutesInTheWindowHasExpired() {
    var window = TripStartWindowEntity.opening(1L, DEPARTS, BUFFER);

    assertThat(window.effectiveDeadline()).isBefore(DEPARTS.plus(Duration.ofMinutes(11)));
  }

  /** 05-5: the extension moves the deadline to +20, exactly once. */
  @Test
  void theExtensionMovesTheDeadlineToTwentyMinutesAfterDeparture() {
    var window = TripStartWindowEntity.opening(1L, DEPARTS, BUFFER);

    assertThat(window.extend(EXTENSION, LIMIT)).isTrue();
    assertThat(window.effectiveDeadline()).isEqualTo(DEPARTS.plus(Duration.ofMinutes(20)));
  }

  /**
   * The extension extends the ORIGINAL buffer, not "now". Extending from the moment of the tap
   * would let a driver who waits until 9:59 buy nearly twenty extra minutes.
   */
  @Test
  void theExtensionIsMeasuredFromTheBufferNotFromTheMomentItWasTaken() {
    var window = TripStartWindowEntity.opening(1L, DEPARTS, BUFFER);
    window.extend(EXTENSION, LIMIT);

    assertThat(window.getExtendedExpiresAt())
        .isEqualTo(window.getBufferExpiresAt().plus(EXTENSION));
  }

  /** 05-6: a second extension is refused and the deadline does not move again. */
  @Test
  void theSecondExtensionIsRefused() {
    var window = TripStartWindowEntity.opening(1L, DEPARTS, BUFFER);
    window.extend(EXTENSION, LIMIT);
    Instant afterFirst = window.effectiveDeadline();

    assertThat(window.extend(EXTENSION, LIMIT)).isFalse();
    assertThat(window.effectiveDeadline()).isEqualTo(afterFirst);
    assertThat(window.hasExtensionRemaining(LIMIT)).isFalse();
  }

  @Test
  void aResolvedWindowCannotBeExtended() {
    var window = TripStartWindowEntity.opening(1L, DEPARTS, BUFFER);
    window.resolve(TripStartWindowEntity.RESOLUTION_STARTED, DEPARTS);

    assertThat(window.extend(EXTENSION, LIMIT)).isFalse();
  }

  /** The sweeper may see a row twice; the second resolution must not overwrite the first. */
  @Test
  void resolutionIsIdempotentAndKeepsTheFirstOutcome() {
    var window = TripStartWindowEntity.opening(1L, DEPARTS, BUFFER);
    Instant started = DEPARTS.plus(Duration.ofMinutes(3));

    window.resolve(TripStartWindowEntity.RESOLUTION_STARTED, started);
    window.resolve(TripStartWindowEntity.RESOLUTION_AUTO_CANCELLED, DEPARTS.plusSeconds(9999));

    assertThat(window.getResolution()).isEqualTo(TripStartWindowEntity.RESOLUTION_STARTED);
    assertThat(window.getResolvedAt()).isEqualTo(started);
  }

  /** A limit of zero disables extensions entirely, without a code change. */
  @Test
  void aZeroExtensionLimitLeavesNoExtensionAvailable() {
    var window = TripStartWindowEntity.opening(1L, DEPARTS, BUFFER);

    assertThat(window.hasExtensionRemaining(0)).isFalse();
    assertThat(window.extend(EXTENSION, 0)).isFalse();
  }
}
