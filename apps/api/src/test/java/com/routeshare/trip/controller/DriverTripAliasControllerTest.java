package com.routeshare.trip.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.service.BookingService;
import com.routeshare.trip.domain.PassengerTripStatus;
import com.routeshare.trip.domain.TripStatus;
import com.routeshare.trip.dto.request.PassengerTripStateTransitionRequest;
import com.routeshare.trip.dto.request.TripTransitionRequest;
import com.routeshare.trip.service.TripService;
import com.routeshare.trip.service.TripStartWindowService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverTripAliasControllerTest {
  @Mock private TripService trips;
  @Mock private BookingService bookings;
  @Mock private TripStartWindowService startWindows;
  @Mock private com.routeshare.trip.service.PickupWaitService pickupWaits;

  @Test
  void driverStartDelegatesToStartedTripTransition() {
    var controller = new DriverTripController(trips, bookings, startWindows, pickupWaits);
    when(trips.transition(40L, new TripTransitionRequest(TripStatus.STARTED)))
        .thenReturn(Map.of("tripId", 40L, "status", "STARTED"));

    var response = controller.start(40L);

    assertThat(response.success()).isTrue();
    assertThat(response.data()).containsEntry("status", "STARTED");
    verify(trips).transition(40L, new TripTransitionRequest(TripStatus.STARTED));
  }

  @Test
  void driverPassengerBoardDelegatesToBoardedPassengerState() {
    var controller = new DriverTripController(trips, bookings, startWindows, pickupWaits);
    when(trips.transitionPassengerState(
            40L,
            30L,
            new PassengerTripStateTransitionRequest(
                PassengerTripStatus.BOARDED, "Driver marked boarded")))
        .thenReturn(Map.of("tripId", 40L, "bookingId", 30L, "status", "BOARDED"));

    var response = controller.board(40L, 30L);

    assertThat(response.success()).isTrue();
    assertThat(response.data()).containsEntry("status", "BOARDED");
    verify(trips)
        .transitionPassengerState(
            org.mockito.Mockito.eq(40L),
            org.mockito.Mockito.eq(30L),
            argThat(req -> req.status() == PassengerTripStatus.BOARDED));
  }
}
