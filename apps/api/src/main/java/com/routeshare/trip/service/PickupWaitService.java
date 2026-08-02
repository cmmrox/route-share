package com.routeshare.trip.service;

import com.routeshare.trip.dto.response.PickupWaitResponse;

/**
 * The pickup-wait clock: 5 minutes from detected arrival, one 5-minute extension, then the seat is
 * released as a no-show.
 *
 * <p>Nothing here starts a wait. Waits are started only by {@link ArrivalDetectionService} from the
 * location trail, which is what stops a driver two streets away from manufacturing a no-show.
 *
 * <p>Ownership is asserted here rather than in the controllers. These calls state and change what a
 * named passenger owes and what goes on her record, so "the caller holds the DRIVER role" is not a
 * sufficient check — it has to be <em>her</em> driver.
 */
public interface PickupWaitService {

  /** D19 / D19b, for the driver running this trip. */
  PickupWaitResponse driverWindow(long tripId, long bookingId);

  /** P38 / P38b, for the passenger whose booking it is. */
  PickupWaitResponse passengerWindow(long bookingId);

  /** Spends the single extension. */
  PickupWaitResponse extend(long tripId, long bookingId);

  /**
   * Releases the seat as a no-show. Refused while the clock is still running, whoever asks: the
   * deadline is the passenger's protection and a driver tap must not shorten it.
   */
  PickupWaitResponse releaseSeat(long tripId, long bookingId);

  /** The passenger boarded — the wait is over and no penalty attaches. */
  void resolveBoarded(long tripId, long bookingId);

  /** Sweep: releases every seat whose wait has run out. */
  int sweepExpired(int batchSize);
}
