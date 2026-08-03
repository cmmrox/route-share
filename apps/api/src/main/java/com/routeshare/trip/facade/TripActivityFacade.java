package com.routeshare.trip.facade;

/** Read-only trip activity seam for delivery rules in other modules. */
public interface TripActivityFacade {

  /** True only while this user is actively driving a live trip. */
  boolean hasActiveDriverTrip(long appUserId);
}
