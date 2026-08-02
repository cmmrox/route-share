package com.routeshare.trip.service;

import com.routeshare.trip.dto.response.StartWindowResponse;
import java.time.Instant;

/**
 * The start-buffer clock: 10 minutes from departure, one 10-minute extension, then auto-cancel.
 *
 * <p>Every deadline here is computed from the injected {@code Clock} and stored. No client
 * timestamp is ever trusted: this clock decides whether a trip is cancelled and whether a missed
 * start is recorded against a driver, and three of those deactivate them.
 */
public interface TripStartWindowService {

  /** Opens the clock when a trip is published or generated. Idempotent per trip. */
  void open(long tripId, Instant departsAt);

  /** D32/D32c/D32b: seconds remaining, whether the extension is still available, and the stakes. */
  StartWindowResponse window(long tripId);

  /**
   * Spends the single extension. D32c replaces the button with a disabled "Extension already used"
   * rather than failing on tap, so the response always carries {@code extensionsRemaining} and this
   * refuses through {@code EXTENSION_ALREADY_USED} only when the driver races their own UI.
   */
  StartWindowResponse extend(long tripId);

  /** Called when the driver starts: the clock stops and nothing is recorded against them. */
  void resolveStarted(long tripId);

  /** Called when the trip is cancelled by a person rather than by the clock. */
  void resolveCancelled(long tripId);

  /**
   * The sweep. Auto-cancels the trip, releases every hold, records a {@code MISSED_START} against
   * the driver and emits {@code trip.autocancelled}.
   *
   * @return the number of trips auto-cancelled.
   */
  int sweepExpired(int batchSize);
}
