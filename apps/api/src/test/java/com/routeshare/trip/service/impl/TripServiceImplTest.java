package com.routeshare.trip.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.trip.domain.PassengerTripStatus;
import com.routeshare.trip.dto.request.PassengerTripStateTransitionRequest;
import com.routeshare.trip.repository.PassengerTripStateRepository;
import com.routeshare.trip.repository.TripRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TripServiceImplTest {
  private final CurrentUserProvider current = org.mockito.Mockito.mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = org.mockito.Mockito.mock(IdentityFacade.class);
  private final TripRepository trips = org.mockito.Mockito.mock(TripRepository.class);
  private final PassengerTripStateRepository passengerStates =
      org.mockito.Mockito.mock(PassengerTripStateRepository.class);
  private final com.routeshare.notification.facade.NotificationFacade notifications =
      org.mockito.Mockito.mock(com.routeshare.notification.facade.NotificationFacade.class);
  private final com.routeshare.payment.facade.PaymentFacade payments =
      org.mockito.Mockito.mock(com.routeshare.payment.facade.PaymentFacade.class);
  private final com.routeshare.trip.service.TripStartWindowService startWindows =
      org.mockito.Mockito.mock(com.routeshare.trip.service.TripStartWindowService.class);
  private final java.time.Clock clock =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-08-02T08:00:00Z"), java.time.ZoneOffset.UTC);
  private final TripServiceImpl service =
      new TripServiceImpl(
          current,
          identityFacade,
          trips,
          passengerStates,
          notifications,
          payments,
          startWindows,
          clock);

  @BeforeEach
  void setUp() {
    var user =
        new CurrentUser("driver-sub", "driver@example.test", null, "Driver", Set.of("DRIVER"));
    var appUser =
        new AppUser(
            9L, UUID.randomUUID(), "driver-sub", "driver@example.test", null, "Driver", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
    when(trips.isOwnedByDriverAppUser(77L, 9L)).thenReturn(true);
  }

  @Test
  void boardsConfirmedPassengerAndReturnsState() {
    var request =
        new PassengerTripStateTransitionRequest(
            PassengerTripStatus.BOARDED, "Passenger entered vehicle");
    when(passengerStates.ensureWaitingPickupStateForConfirmedBooking(77L, 100L)).thenReturn(1);
    when(passengerStates.findStatusForUpdate(77L, 100L))
        .thenReturn(Optional.of(PassengerTripStatus.WAITING_PICKUP));
    when(passengerStates.updateStatus(77L, 100L, PassengerTripStatus.BOARDED)).thenReturn(1);

    var response = service.transitionPassengerState(77L, 100L, request);

    assertThat(response).containsEntry("tripId", 77L);
    assertThat(response).containsEntry("bookingId", 100L);
    assertThat(response).containsEntry("status", "BOARDED");
    verify(passengerStates).ensureWaitingPickupStateForConfirmedBooking(77L, 100L);
    verify(passengerStates).updateStatus(77L, 100L, PassengerTripStatus.BOARDED);
  }

  @Test
  void rejectsNoShowAfterPassengerAlreadyBoarded() {
    var request = new PassengerTripStateTransitionRequest(PassengerTripStatus.NO_SHOW, "Too late");
    when(passengerStates.ensureWaitingPickupStateForConfirmedBooking(77L, 100L)).thenReturn(0);
    when(passengerStates.findStatusForUpdate(77L, 100L))
        .thenReturn(Optional.of(PassengerTripStatus.BOARDED));

    assertThatThrownBy(() -> service.transitionPassengerState(77L, 100L, request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid passenger trip transition");

    verify(passengerStates, never()).updateStatus(anyLong(), anyLong(), any());
  }
}
