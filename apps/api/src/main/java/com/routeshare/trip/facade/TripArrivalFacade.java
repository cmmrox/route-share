package com.routeshare.trip.facade;

/**
 * How {@code location} hands an accepted sample to the arrival detector.
 *
 * <p>The seam matters: {@code location} owns the sample stream and knows nothing about pickup
 * waits, no-shows or what a wait costs. It reports that a trip moved; {@code trip} decides what
 * that means.
 */
public interface TripArrivalFacade {

  /**
   * @see com.routeshare.trip.service.ArrivalDetectionService#onDriverLocation(long)
   */
  int onDriverLocation(long tripId);
}
