package com.routeshare.trip.service;

import com.routeshare.trip.dto.response.CancellationTermsResponse;

/**
 * The driver-late grace, and the cancellation terms it feeds.
 *
 * <p>Ten minutes past her promised pickup with no driver in sight unlocks a free cancel — and
 * nothing is recorded against her for taking it.
 */
public interface DriverLateGraceService {

  /** Opened when a booking is confirmed, from a server-derived promised pickup time. */
  void openForBooking(long bookingId);

  /** P26 and P34's single source of truth. */
  CancellationTermsResponse cancellationTerms(long bookingId);

  /** She got in the car: the grace is over and never unlocked. */
  void resolvePickedUp(long bookingId);

  /** She cancelled: recorded as free or not according to whether the grace had unlocked. */
  void resolveCancelled(long bookingId);

  /** Sweep: unlocks the free cancel and tells her it is available. */
  int sweepExpired(int batchSize);
}
