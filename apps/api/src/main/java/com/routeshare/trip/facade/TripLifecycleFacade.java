package com.routeshare.trip.facade;

/**
 * How {@code booking} asks for the trip behind an occurrence it has just confirmed a seat on.
 *
 * <p>Booking owns seats and money; trips and their clocks belong to {@code trip}. This is the seam
 * between the two, so booking never learns what a start window is.
 */
public interface TripLifecycleFacade {

  /**
   * @see com.routeshare.trip.service.TripLifecycleService#ensureTripForBookedOccurrence(long)
   */
  long ensureTripForBookedOccurrence(long routeOccurrenceId);
}
