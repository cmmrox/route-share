package com.routeshare.booking.service.impl;

import com.routeshare.booking.entity.BookingSeatEntity;
import com.routeshare.booking.repository.BookingSeatRepository;
import com.routeshare.booking.service.SeatHoldService;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.routing.domain.ApprovalMode;
import com.routeshare.routing.service.SeatInventoryService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatHoldServiceImpl implements SeatHoldService {

  private final BookingSeatRepository holds;
  private final SeatInventoryService inventory;
  private final MeterRegistry meters;
  private final Clock clock;

  @Override
  @Transactional
  public List<HeldSeat> hold(
      long bookingId, long routeOccurrenceId, List<Long> requestedSeatIds, int seatCount) {
    // Slots must exist before they can be held. An occurrence published before named seats, or one
    // whose generation failed, would otherwise be quietly unbookable.
    inventory.generateFor(routeOccurrenceId);

    List<Long> seatIds =
        inventory.resolveSeatsForBooking(routeOccurrenceId, requestedSeatIds, seatCount);
    try {
      holds.saveAll(seatIds.stream().map(id -> BookingSeatEntity.hold(bookingId, id)).toList());
      // Flushed here rather than at commit so the conflict surfaces as a seat error the rider can
      // act on, instead of an opaque failure after everything else has already been written.
      holds.flush();
    } catch (DataIntegrityViolationException taken) {
      meters.counter("routeshare_seat_race_conflicts_total").increment();
      log.info("seat race lost on occurrence {} for booking {}", routeOccurrenceId, bookingId);
      throw new GateConflictException(
          "SEATS_TAKEN",
          "Someone booked that seat a moment before you. Pick another, or try a different trip.",
          "/passenger/bookings/" + bookingId + "/alternatives");
    }
    return heldSeats(bookingId);
  }

  @Override
  @Transactional
  public int release(long bookingId) {
    var live = holds.findLiveHolds(bookingId);
    if (live.isEmpty()) {
      return 0;
    }
    live.forEach(hold -> hold.release(clock.instant()));
    holds.saveAll(live);
    return live.size();
  }

  @Override
  @Transactional(readOnly = true)
  public List<HeldSeat> heldSeats(long bookingId) {
    return holds.findHeldSeats(bookingId).stream()
        .map(
            row ->
                new HeldSeat(
                    row.getSeatId(), row.getSlotIndex(), row.getLabel(), row.getSubLabel()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ApprovalMode approvalModeFor(long routeOccurrenceId) {
    return inventory.approvalModeFor(routeOccurrenceId);
  }
}
