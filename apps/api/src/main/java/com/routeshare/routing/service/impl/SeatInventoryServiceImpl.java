package com.routeshare.routing.service.impl;

import com.routeshare.common.errors.GateConflictException;
import com.routeshare.routing.domain.SeatPlan;
import com.routeshare.routing.dto.response.SeatMapResponse;
import com.routeshare.routing.entity.RouteOccurrenceSeatEntity;
import com.routeshare.routing.repository.RouteOccurrenceSeatRepository;
import com.routeshare.routing.service.SeatInventoryService;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatInventoryServiceImpl implements SeatInventoryService {

  private final RouteOccurrenceSeatRepository seats;

  @Override
  @Transactional
  public int generateFor(long routeOccurrenceId) {
    if (seats.countSlots(routeOccurrenceId) > 0) {
      return 0;
    }
    int capacity =
        seats
            .findCapacity(routeOccurrenceId)
            .orElseThrow(() -> new NoSuchElementException("Route occurrence not found"));

    List<RouteOccurrenceSeatEntity> rows =
        SeatPlan.slots(capacity).stream()
            .map(
                slot ->
                    RouteOccurrenceSeatEntity.of(
                        routeOccurrenceId, slot.index(), slot.label(), slot.subLabel()))
            .toList();
    try {
      return seats.saveAll(rows).size();
    } catch (DataIntegrityViolationException raced) {
      // Two publishes of the same occurrence, or a generate racing a backfill. The unique index
      // already holds the answer; generating twice is not an error, it is a no-op.
      log.info("seat slots for occurrence {} were generated concurrently", routeOccurrenceId);
      return 0;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public com.routeshare.routing.domain.ApprovalMode approvalModeFor(long routeOccurrenceId) {
    return seats
        .findOccurrenceContext(routeOccurrenceId)
        .map(row -> com.routeshare.routing.domain.ApprovalMode.of(row.getApprovalMode()))
        .orElse(com.routeshare.routing.domain.ApprovalMode.APPROVE_EACH);
  }

  @Override
  @Transactional(readOnly = true)
  public SeatMapResponse seatMap(long routeOccurrenceId) {
    var rows = seats.findSeatMap(routeOccurrenceId);
    if (rows.isEmpty()) {
      throw new NoSuchElementException("This trip has no seat map");
    }
    List<SeatMapResponse.Seat> mapped =
        rows.stream()
            .map(
                row ->
                    new SeatMapResponse.Seat(
                        row.getSeatId(),
                        row.getSlotIndex(),
                        row.getLabel(),
                        row.getSubLabel(),
                        Boolean.TRUE.equals(row.getTaken())))
            .toList();
    int free = (int) mapped.stream().filter(seat -> !seat.taken()).count();
    return new SeatMapResponse(routeOccurrenceId, mapped.size(), free, mapped);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Long> resolveSeatsForBooking(
      long routeOccurrenceId, List<Long> requestedSeatIds, int seatCount) {
    if (seatCount < 1) {
      throw new IllegalArgumentException("A booking must take at least one seat");
    }

    if (requestedSeatIds != null && !requestedSeatIds.isEmpty()) {
      List<Long> distinct = requestedSeatIds.stream().distinct().toList();
      if (distinct.size() != seatCount) {
        throw new IllegalArgumentException(
            "Choose exactly " + seatCount + " seat" + (seatCount == 1 ? "" : "s"));
      }
      // A slot id from another trip would otherwise hold a seat in a car this rider is not in.
      if (seats.countSeatsOnOccurrence(routeOccurrenceId, distinct) != distinct.size()) {
        throw new IllegalArgumentException("Those seats are not on this trip");
      }
      return distinct;
    }

    // No choice made — the lowest free slots. Checked here so a sold-out trip is refused with the
    // seat-shaped error rather than by the hold blowing up on a constraint.
    List<Long> free = seats.findFreeSeatIds(routeOccurrenceId, seatCount);
    if (free.size() < seatCount) {
      throw new GateConflictException(
          "SEATS_TAKEN",
          "Those seats have just been taken.",
          "/passenger/route-occurrences/" + routeOccurrenceId + "/seats");
    }
    return new ArrayList<>(free);
  }
}
