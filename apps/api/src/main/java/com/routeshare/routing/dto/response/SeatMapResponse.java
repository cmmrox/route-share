package com.routeshare.routing.dto.response;

import java.util.List;

/**
 * P08's seat picker.
 *
 * <p>There is no price per slot and there never will be: P08 says every seat costs the same, and a
 * field for it here is the first step towards a screen that lies.
 */
public record SeatMapResponse(
    long routeOccurrenceId, int capacity, int seatsAvailable, List<Seat> seats) {

  public record Seat(long seatId, int slotIndex, String label, String subLabel, boolean taken) {}
}
