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

  /**
   * Opens this passenger's driver-late grace from a server-derived promised pickup time.
   *
   * <p>Separate from the trip's own clock on purpose (P35): the start buffer runs from departure
   * and protects the driver, this runs from her promised pickup and protects her.
   */
  void openLateGraceForBooking(long bookingId);

  /** Closes the grace when she cancels, recording whether it was free because he was late. */
  void resolveLateGraceOnCancel(long bookingId);
}
