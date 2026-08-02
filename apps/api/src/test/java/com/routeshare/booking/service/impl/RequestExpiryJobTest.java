package com.routeshare.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.BookingStatusHistoryRepository;
import com.routeshare.booking.service.SeatHoldService;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.payment.facade.PaymentFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D16: the request the driver never answered.
 *
 * <p>Two properties matter more than the sweep itself. The seat must go back — a hold left behind
 * an expired request removes a seat from a car for ever, and nobody involved ever notices. And a
 * driver who approved one second before the sweep read the row must not have his passenger expired
 * underneath him, which is why the guard is the status predicate in the update rather than anything
 * this loop can see.
 */
class RequestExpiryJobTest {
  private static final Instant NOW = Instant.parse("2026-08-02T09:00:00Z");

  private final BookingRepository bookings = mock(BookingRepository.class);
  private final BookingStatusHistoryRepository statusHistory =
      mock(BookingStatusHistoryRepository.class);
  private final SeatHoldService seatHolds = mock(SeatHoldService.class);
  private final PaymentFacade payments = mock(PaymentFacade.class);
  private final NotificationFacade notifications = mock(NotificationFacade.class);
  private final DomainEventPublisher events = mock(DomainEventPublisher.class);

  private final BookingExpiryServiceImpl service =
      new BookingExpiryServiceImpl(
          bookings,
          statusHistory,
          seatHolds,
          payments,
          notifications,
          events,
          new SimpleMeterRegistry(),
          Clock.fixed(NOW, ZoneOffset.UTC));

  private BookingRepository.OpenBookingRow lapsed(long bookingId) {
    var row = mock(BookingRepository.OpenBookingRow.class);
    when(row.getBookingId()).thenReturn(bookingId);
    when(row.getPassengerAppUserId()).thenReturn(100L);
    when(row.getRouteOccurrenceId()).thenReturn(44L);
    when(row.getStatus()).thenReturn("REQUESTED");
    return row;
  }

  @Test
  @DisplayName("07-7: a lapsed request expires, releases its seat and voids the hold")
  void expiryReleasesTheSeatAndTheMoney() {
    var row = lapsed(1L);
    when(bookings.findExpiredRequests(any(), anyInt())).thenReturn(List.of(row));
    when(bookings.markExpired(1L, NOW)).thenReturn(1);

    assertThat(service.sweepExpired(200)).isEqualTo(1);

    verify(seatHolds).release(1L);
    verify(payments).voidForBooking(1L, "REQUEST_EXPIRED");
    verify(statusHistory)
        .recordTransition(1L, "REQUESTED", "EXPIRED", 100L, "The driver did not reply in time");
    verify(notifications).notifyUser(anyLong(), any(), any(), any(), any());
    verify(events).publish(any());
  }

  @Test
  @DisplayName("A request approved between the read and the update is left alone")
  void approvedUnderneathTheSweepIsUntouched() {
    var row = lapsed(1L);
    when(bookings.findExpiredRequests(any(), anyInt())).thenReturn(List.of(row));
    // The row moved to CONFIRMED after the sweep read it, so the guarded update matches nothing.
    when(bookings.markExpired(1L, NOW)).thenReturn(0);

    assertThat(service.sweepExpired(200)).isZero();

    verify(seatHolds, never()).release(anyLong());
    verify(payments, never()).voidForBooking(anyLong(), any());
    verify(notifications, never()).notifyUser(anyLong(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Nothing to sweep is not an error")
  void anEmptySweepIsFine() {
    when(bookings.findExpiredRequests(any(), anyInt())).thenReturn(List.of());
    assertThat(service.sweepExpired(200)).isZero();
    verify(events, never()).publish(any());
  }

  @Test
  @DisplayName("One bad row does not stop the rest of the batch")
  void theBatchContinuesPastARefusedRow() {
    var rows = List.of(lapsed(1L), lapsed(2L), lapsed(3L));
    when(bookings.findExpiredRequests(any(), anyInt())).thenReturn(rows);
    when(bookings.markExpired(1L, NOW)).thenReturn(1);
    when(bookings.markExpired(2L, NOW)).thenReturn(0);
    when(bookings.markExpired(3L, NOW)).thenReturn(1);

    assertThat(service.sweepExpired(200)).isEqualTo(2);
    verify(seatHolds).release(1L);
    verify(seatHolds).release(3L);
    verify(seatHolds, never()).release(2L);
  }

  @Test
  @DisplayName("The job is registered under the name the scheduler and job_run both use")
  void jobIsNamedForTheScheduler() {
    var job = new com.routeshare.booking.job.ScheduledRequestExpiryJob(service, 200);
    assertThat(job.name()).isEqualTo("scheduled-request-expiry");
  }
}
