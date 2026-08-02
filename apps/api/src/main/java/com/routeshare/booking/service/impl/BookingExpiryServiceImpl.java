package com.routeshare.booking.service.impl;

import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.BookingStatusHistoryRepository;
import com.routeshare.booking.service.BookingExpiryService;
import com.routeshare.booking.service.SeatHoldService;
import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.payment.facade.PaymentFacade;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryServiceImpl implements BookingExpiryService {

  private final BookingRepository bookings;
  private final BookingStatusHistoryRepository statusHistory;
  private final SeatHoldService seatHolds;
  private final PaymentFacade payments;
  private final NotificationFacade notifications;
  private final DomainEventPublisher events;
  private final MeterRegistry meters;
  private final Clock clock;

  @Override
  @Transactional
  public int sweepExpired(int batchSize) {
    Instant now = clock.instant();
    int expired = 0;
    for (var row : bookings.findExpiredRequests(now, batchSize)) {
      // The status predicate inside markExpired is the guard, not this loop: a driver who approved
      // one second before the sweep read the row must not have his passenger expired underneath
      // him, and the sweep cannot know that from what it read.
      if (bookings.markExpired(row.getBookingId(), now) != 1) {
        continue;
      }
      statusHistory.recordTransition(
          row.getBookingId(),
          "REQUESTED",
          "EXPIRED",
          row.getPassengerAppUserId(),
          "The driver did not reply in time");

      // Seats first, then money. A seat left held is inventory destroyed; a hold left on a card is
      // visible to the person it belongs to and is the thing she will call about.
      seatHolds.release(row.getBookingId());
      payments.voidForBooking(row.getBookingId(), "REQUEST_EXPIRED");

      notifications.notifyUser(
          row.getPassengerAppUserId(),
          "BOOKING_REQUEST_EXPIRED",
          "Your request lapsed",
          "Your driver did not reply in time. Nothing was charged — here are other trips on your"
              + " route.",
          Map.of("bookingId", String.valueOf(row.getBookingId())));

      events.publish(
          DomainEvent.of(
              "booking.expired",
              "booking",
              String.valueOf(row.getBookingId()),
              """
              {"bookingId":%d,"routeOccurrenceId":%s,"expiredAt":"%s"}"""
                  .formatted(row.getBookingId(), row.getRouteOccurrenceId(), now)));
      expired++;
    }
    if (expired > 0) {
      meters.counter("routeshare_requests_expired_total").increment(expired);
      log.info("{} booking requests lapsed", expired);
    }
    return expired;
  }
}
