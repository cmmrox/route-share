package com.routeshare.trip.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.payment.facade.PaymentFacade;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.service.ReliabilityService;
import com.routeshare.trip.domain.TripStatus;
import com.routeshare.trip.entity.TripEntity;
import com.routeshare.trip.entity.TripStartWindowEntity;
import com.routeshare.trip.repository.TripRepository;
import com.routeshare.trip.repository.TripStartWindowRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/**
 * What the sweeper is allowed to cancel.
 *
 * <p>The window is a convenience, not the record. A trip that has moved is authoritative about
 * that, and a sweep that trusted only the window would cancel a car with passengers in it and void
 * holds that were captured at the start — the single worst outcome this slice can produce.
 */
class TripStartWindowSweepTest {
  private static final Instant DEPARTS = Instant.parse("2026-08-02T09:00:00Z");
  private static final Instant PAST_DEADLINE = DEPARTS.plus(Duration.ofMinutes(11));

  private final TripStartWindowRepository windows = mock(TripStartWindowRepository.class);
  private final TripRepository trips = mock(TripRepository.class);
  private final PolicySettingService policy = mock(PolicySettingService.class);
  private final ReliabilityService reliability = mock(ReliabilityService.class);
  private final PaymentFacade payments = mock(PaymentFacade.class);
  private final com.routeshare.penalty.facade.PenaltyFacade penalties =
      mock(com.routeshare.penalty.facade.PenaltyFacade.class);

  private final TripStartWindowServiceImpl service =
      new TripStartWindowServiceImpl(
          windows,
          trips,
          policy,
          reliability,
          payments,
          penalties,
          mock(com.routeshare.booking.facade.BookingFacade.class),
          mock(com.routeshare.rewards.facade.RewardsFacade.class),
          new SimpleMeterRegistry(),
          Clock.fixed(PAST_DEADLINE, ZoneOffset.UTC));

  private TripStartWindowEntity expiredWindow() {
    return TripStartWindowEntity.opening(77L, DEPARTS, Duration.ofMinutes(10));
  }

  private void sweepFinds(TripStartWindowEntity window, TripStatus tripStatus) {
    when(windows.claimExpired(any(Instant.class), any(Pageable.class))).thenReturn(List.of(window));
    TripEntity trip = mock(TripEntity.class);
    when(trip.getStatus()).thenReturn(tripStatus);
    when(trips.findById(77L)).thenReturn(Optional.of(trip));
  }

  /** 05-4: nobody started it, so it goes. */
  @Test
  void aScheduledTripPastItsDeadlineIsAutoCancelled() {
    var window = expiredWindow();
    sweepFinds(window, TripStatus.SCHEDULED);
    when(windows.findLiveBookingIds(77L)).thenReturn(List.of(100L, 101L));
    when(windows.findDriverAppUserId(77L)).thenReturn(Optional.of(9L));

    assertThat(service.sweepExpired(200)).isEqualTo(1);
    assertThat(window.getResolution()).isEqualTo(TripStartWindowEntity.RESOLUTION_AUTO_CANCELLED);
    verify(payments).voidForBooking(100L, "TRIP_AUTO_CANCELLED");
    verify(payments).voidForBooking(101L, "TRIP_AUTO_CANCELLED");
    verify(reliability)
        .record(
            anyLong(),
            any(),
            org.mockito.ArgumentMatchers.eq(ReliabilityEventType.MISSED_START),
            any(),
            any(),
            any());
  }

  /**
   * The bug this test exists for: before the start path resolved its window, a trip started at +5
   * was still cancelled at +11 — after its cards had been captured.
   */
  @Test
  void aTripThatHasAlreadyStartedIsNeverAutoCancelled() {
    var window = expiredWindow();
    sweepFinds(window, TripStatus.STARTED);

    assertThat(service.sweepExpired(200)).isZero();
    assertThat(window.getResolution()).isEqualTo(TripStartWindowEntity.RESOLUTION_STARTED);
    verify(payments, never()).voidForBooking(anyLong(), anyString());
    verify(reliability, never()).record(anyLong(), any(), any(), any(), any(), any());
  }

  /** A trip already under way in a later state is equally not the sweeper's business. */
  @Test
  void aTripCarryingPassengersIsNeverAutoCancelled() {
    var window = expiredWindow();
    sweepFinds(window, TripStatus.PASSENGER_ONBOARD);

    assertThat(service.sweepExpired(200)).isZero();
    verify(payments, never()).voidForBooking(anyLong(), anyString());
  }

  /**
   * Cancelled by hand, so the driver keeps their record: an AUTO_CANCELLED resolution here would
   * record a missed start against somebody who cancelled properly.
   */
  @Test
  void aManuallyCancelledTripResolvesAsCancelledAndRecordsNoMissedStart() {
    var window = expiredWindow();
    sweepFinds(window, TripStatus.CANCELLED);

    assertThat(service.sweepExpired(200)).isZero();
    assertThat(window.getResolution()).isEqualTo(TripStartWindowEntity.RESOLUTION_CANCELLED);
    verify(reliability, never()).record(anyLong(), any(), any(), any(), any(), any());
  }
}
