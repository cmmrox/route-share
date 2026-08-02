package com.routeshare.trip.facade;

import com.routeshare.trip.dto.response.PickupWaitResponse;

/**
 * The passenger-facing views of clocks that live in {@code trip}, read through {@code booking}
 * because a passenger knows her booking and not the trip behind it.
 */
public interface TripTimerFacade {

  /** P38 / P38b. */
  PickupWaitResponse pickupWindowForBooking(long bookingId);

  /** P26 and P34's single source: whether a cancel is free right now, and why. */
  com.routeshare.trip.dto.response.CancellationTermsResponse cancellationTerms(long bookingId);
}
