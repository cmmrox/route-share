package com.routeshare.trip.domain;

import java.util.Map;
import java.util.Set;

public class PassengerTripStateMachine {
  private static final Map<PassengerTripStatus, Set<PassengerTripStatus>> ALLOWED =
      Map.of(
          PassengerTripStatus.WAITING_PICKUP,
          Set.of(PassengerTripStatus.BOARDED, PassengerTripStatus.NO_SHOW),
          PassengerTripStatus.BOARDED,
          Set.of(PassengerTripStatus.DROPPED_OFF),
          PassengerTripStatus.NO_SHOW,
          Set.of(),
          PassengerTripStatus.DROPPED_OFF,
          Set.of());

  public void assertTransition(PassengerTripStatus from, PassengerTripStatus to) {
    if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
      throw new IllegalStateException(
          "Invalid passenger trip transition from " + from + " to " + to);
    }
  }
}
