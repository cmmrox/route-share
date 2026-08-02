package com.routeshare.trip.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.trip.repository.TripRepository;
import com.routeshare.trip.service.TripStartWindowService;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The seam that was missing entirely: nothing in the application created a trip, so nothing ever
 * opened a start window and the sweeper swept an empty table.
 */
class TripLifecycleServiceImplTest {
  private static final Instant DEPARTS = Instant.parse("2026-08-02T09:00:00Z");

  private final TripRepository trips = mock(TripRepository.class);
  private final TripStartWindowService startWindows = mock(TripStartWindowService.class);
  private final TripLifecycleServiceImpl service =
      new TripLifecycleServiceImpl(trips, startWindows);

  @Test
  void firstConfirmedBookingCreatesTheTripAndOpensItsWindowFromTheOccurrenceDeparture() {
    when(trips.insertTripForOccurrence(55L)).thenReturn(1);
    when(trips.findTripIdByRouteOccurrenceId(55L)).thenReturn(Optional.of(77L));
    when(trips.findScheduledDepartureForOccurrence(55L)).thenReturn(Optional.of(DEPARTS));

    assertThat(service.ensureTripForBookedOccurrence(55L)).isEqualTo(77L);
    verify(startWindows).open(77L, DEPARTS);
  }

  /**
   * The losing side of the race two passengers run for the last two seats. The insert does nothing
   * and the read still answers, so both bookings attach to one trip rather than one of them failing
   * or a second trip appearing on the same occurrence.
   */
  @Test
  void aSecondBookingOnTheSameOccurrenceGetsTheSameTrip() {
    when(trips.insertTripForOccurrence(55L)).thenReturn(0);
    when(trips.findTripIdByRouteOccurrenceId(55L)).thenReturn(Optional.of(77L));
    when(trips.findScheduledDepartureForOccurrence(55L)).thenReturn(Optional.of(DEPARTS));

    assertThat(service.ensureTripForBookedOccurrence(55L)).isEqualTo(77L);
    // open() is idempotent in its own right; this asserts the second booking does not attempt to
    // reopen a window whose deadline may already have been extended.
    verify(startWindows).open(77L, DEPARTS);
  }

  @Test
  void anOccurrenceThatDoesNotExistIsRefusedRatherThanSilentlySkipped() {
    when(trips.insertTripForOccurrence(404L)).thenReturn(0);
    when(trips.findTripIdByRouteOccurrenceId(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.ensureTripForBookedOccurrence(404L))
        .isInstanceOf(NoSuchElementException.class);
    verify(startWindows, never()).open(anyLong(), org.mockito.ArgumentMatchers.any());
  }

  /**
   * A window with no departure behind it would be a clock with an invented deadline. Better to fail
   * the booking than to open one.
   */
  @Test
  void anOccurrenceWithNoScheduledDepartureCannotOpenAWindow() {
    when(trips.insertTripForOccurrence(55L)).thenReturn(1);
    when(trips.findTripIdByRouteOccurrenceId(55L)).thenReturn(Optional.of(77L));
    when(trips.findScheduledDepartureForOccurrence(55L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.ensureTripForBookedOccurrence(55L))
        .isInstanceOf(IllegalStateException.class);
    verify(startWindows, never()).open(anyLong(), org.mockito.ArgumentMatchers.any());
  }
}
