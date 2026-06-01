package com.routeshare.trip.domain;

import java.util.*;

public class TripStateMachine {
  private static final Map<TripStatus, Set<TripStatus>> ALLOWED =
      Map.of(
          TripStatus.SCHEDULED,
          Set.of(TripStatus.STARTED, TripStatus.CANCELLED),
          TripStatus.STARTED,
          Set.of(TripStatus.ARRIVED_PICKUP, TripStatus.CANCELLED),
          TripStatus.ARRIVED_PICKUP,
          Set.of(TripStatus.PASSENGER_ONBOARD, TripStatus.CANCELLED),
          TripStatus.PASSENGER_ONBOARD,
          Set.of(TripStatus.COMPLETED, TripStatus.CANCELLED),
          TripStatus.COMPLETED,
          Set.of(),
          TripStatus.CANCELLED,
          Set.of());

  public void assertTransition(TripStatus from, TripStatus to) {
    if (!ALLOWED.getOrDefault(from, Set.of()).contains(to))
      throw new IllegalStateException("Invalid trip transition from " + from + " to " + to);
  }
}
