package com.routeshare.routing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D09: a published trip is editable until its first seat sells.
 *
 * <p>The rule is computed from facts already true of the row, and these cases exist to keep it that
 * way. A stored boolean would pass every one of them on the day it was written and drift the first
 * time a booking was cancelled by a path that forgot to reset it.
 */
class TripFreezeTest {

  @Test
  @DisplayName("A published trip nobody has booked is still editable")
  void editableBeforeAnyBooking() {
    assertThat(TripEditability.isEditable("PUBLISHED", 0)).isTrue();
  }

  @Test
  @DisplayName("One booked seat freezes it")
  void frozenByTheFirstBooking() {
    assertThat(TripEditability.isEditable("PUBLISHED", 1)).isFalse();
  }

  @Test
  @DisplayName("Cancelling that booking makes it editable again — the rule has nothing to reset")
  void editableAgainWhenTheHoldGoesBack() {
    assertThat(TripEditability.isEditable("PUBLISHED", 1)).isFalse();
    assertThat(TripEditability.isEditable("PUBLISHED", 0)).isTrue();
  }

  @Test
  @DisplayName("A trip that is not published is never editable, whatever its seats say")
  void unpublishedIsNeverEditable() {
    for (String status : List.of("CANCELLED", "COMPLETED", "DRAFT")) {
      assertThat(TripEditability.isEditable(status, 0)).as(status).isFalse();
    }
  }

  @Test
  @DisplayName("The freeze reason counts the seats, singular and plural")
  void freezeReasonReadsCorrectly() {
    assertThat(TripEditability.freezeReason(1))
        .isEqualTo("Someone has booked a seat, so the details are now fixed.");
    assertThat(TripEditability.freezeReason(3)).startsWith("3 seats are booked");
  }

  @Test
  @DisplayName("P08: a CAR has one front seat beside the driver and the rest in the rear row")
  void seatPlanNamesTheFrontSeatOnce() {
    var slots = SeatPlan.slots(3);

    assertThat(slots).hasSize(3);
    assertThat(slots.get(0).index()).isEqualTo(1);
    assertThat(slots.get(0).label()).isEqualTo("Front seat");
    assertThat(slots.get(0).subLabel()).isEqualTo("Beside the driver");
    assertThat(slots.subList(1, 3))
        .allSatisfy(
            slot -> {
              assertThat(slot.label()).isEqualTo("Back seat");
              assertThat(slot.subLabel()).isEqualTo("Rear row");
            });
  }

  @Test
  @DisplayName("A six-seat van still has exactly one front seat")
  void onlyOneFrontSeatAtAnyCapacity() {
    for (int capacity = 1; capacity <= 6; capacity++) {
      long front =
          SeatPlan.slots(capacity).stream().filter(s -> s.label().equals("Front seat")).count();
      assertThat(front).as("capacity " + capacity).isEqualTo(1);
    }
  }

  @Test
  @DisplayName("A vehicle carrying no passengers cannot be published")
  void zeroCapacityIsRefused() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> SeatPlan.slots(0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("An unset approval mode is the cautious one, never instant-book")
  void approvalModeDefaultsToApproveEach() {
    assertThat(ApprovalMode.of(null)).isEqualTo(ApprovalMode.APPROVE_EACH);
    assertThat(ApprovalMode.of("INSTANT").confirmsImmediately()).isTrue();
    assertThat(ApprovalMode.APPROVE_EACH.confirmsImmediately()).isFalse();
  }
}
