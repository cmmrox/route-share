package com.routeshare.routing.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * The seats in a car, named. Pure, so the labels a rider chooses between can be tested without a
 * database.
 *
 * <p>P08 offers exactly one distinction — the front seat beside the driver, or a place in the rear
 * row — because that is the only one that changes the ride. There is no seat map to draw and no
 * diagram to keep in step with a vehicle's real interior; a label and a sub-label are the whole
 * model, and they come straight from {@code seatSlots} in the prototype.
 *
 * <p><b>Every slot costs the same.</b> P08 states it, and nothing here carries a price for that
 * reason: a seat that could be priced is a seat that eventually will be, and then the screen is
 * wrong.
 */
public final class SeatPlan {
  public static final String FRONT_LABEL = "Front seat";
  public static final String FRONT_SUB = "Beside the driver";
  public static final String BACK_LABEL = "Back seat";
  public static final String BACK_SUB = "Rear row";

  private SeatPlan() {}

  /**
   * @param capacity passenger seats the vehicle's class allows, not counting the driver
   * @return one slot per seat, numbered from 1; slot 1 is always the front seat
   */
  public static List<Slot> slots(int capacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("A vehicle carrying no passengers cannot be published");
    }
    List<Slot> slots = new ArrayList<>(capacity);
    for (int index = 1; index <= capacity; index++) {
      slots.add(
          index == 1
              ? new Slot(index, FRONT_LABEL, FRONT_SUB)
              : new Slot(index, BACK_LABEL, BACK_SUB));
    }
    return List.copyOf(slots);
  }

  /** One nameable place in the car. Carries no price, deliberately — see the class note. */
  public record Slot(int index, String label, String subLabel) {}
}
