package com.routeshare.trip.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PassengerTripStateMachineTest {
  private final PassengerTripStateMachine stateMachine = new PassengerTripStateMachine();

  @Test
  void allowsPassengerBoardingAndDropOffLifecycle() {
    assertThatCode(
            () ->
                stateMachine.assertTransition(
                    PassengerTripStatus.WAITING_PICKUP, PassengerTripStatus.BOARDED))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                stateMachine.assertTransition(
                    PassengerTripStatus.BOARDED, PassengerTripStatus.DROPPED_OFF))
        .doesNotThrowAnyException();
  }

  @Test
  void allowsNoShowOnlyBeforePassengerBoards() {
    assertThatCode(
            () ->
                stateMachine.assertTransition(
                    PassengerTripStatus.WAITING_PICKUP, PassengerTripStatus.NO_SHOW))
        .doesNotThrowAnyException();

    assertThatThrownBy(
            () ->
                stateMachine.assertTransition(
                    PassengerTripStatus.BOARDED, PassengerTripStatus.NO_SHOW))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid passenger trip transition");
  }

  @Test
  void rejectsTerminalStateChanges() {
    assertThatThrownBy(
            () ->
                stateMachine.assertTransition(
                    PassengerTripStatus.DROPPED_OFF, PassengerTripStatus.BOARDED))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid passenger trip transition");
  }
}
