package com.routeshare.trip.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TripStateMachineTest {
  private final TripStateMachine sm = new TripStateMachine();

  @Test
  void allowsForwardTripLifecycle() {
    assertThatCode(() -> sm.assertTransition(TripStatus.SCHEDULED, TripStatus.STARTED))
        .doesNotThrowAnyException();
    assertThatCode(() -> sm.assertTransition(TripStatus.PASSENGER_ONBOARD, TripStatus.COMPLETED))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsInvalidBackwardTransition() {
    assertThatThrownBy(() -> sm.assertTransition(TripStatus.COMPLETED, TripStatus.STARTED))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid trip transition");
  }
}
