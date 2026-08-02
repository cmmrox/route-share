package com.routeshare.trip.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.reliability.domain.ReliabilityEventType;
import com.routeshare.reliability.entity.MonthlyCounterEntity;
import com.routeshare.reliability.service.ReliabilityService;
import com.routeshare.routing.facade.RoutingFacade;
import com.routeshare.trip.domain.PassengerTripStatus;
import com.routeshare.trip.entity.PickupWaitEntity;
import com.routeshare.trip.repository.PassengerTripStateRepository;
import com.routeshare.trip.repository.PickupWaitRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

/**
 * What a no-show release does, and — more importantly — when it refuses to.
 *
 * <p>Every case here ends in somebody being charged a fee and marked absent, so the refusals are
 * the point: a driver must not be able to release early, and must not be able to touch a trip that
 * is not his.
 */
class PickupWaitServiceImplTest {
  private static final Instant ARRIVED = Instant.parse("2026-08-02T09:20:00Z");
  private static final Instant DEADLINE = ARRIVED.plus(Duration.ofMinutes(5));
  private static final long TRIP = 77L;
  private static final long BOOKING = 100L;
  private static final long DRIVER_APP_USER = 9L;
  private static final long PASSENGER_APP_USER = 12L;

  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final PickupWaitRepository waits = mock(PickupWaitRepository.class);
  private final PassengerTripStateRepository passengerStates =
      mock(PassengerTripStateRepository.class);
  private final RoutingFacade routing = mock(RoutingFacade.class);
  private final PolicySettingService policy = mock(PolicySettingService.class);
  private final ReliabilityService reliability = mock(ReliabilityService.class);
  private final NotificationFacade notifications = mock(NotificationFacade.class);
  private final DomainEventPublisher events = mock(DomainEventPublisher.class);

  private final com.routeshare.penalty.facade.PenaltyFacade penalties =
      org.mockito.Mockito.mock(com.routeshare.penalty.facade.PenaltyFacade.class);

  private PickupWaitServiceImpl serviceAt(Instant now) {
    var user = new CurrentUser("driver-sub", "d@example.test", null, "Driver", Set.of("DRIVER"));
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user))
        .thenReturn(
            new AppUser(
                DRIVER_APP_USER,
                UUID.randomUUID(),
                "driver-sub",
                "d@example.test",
                null,
                "Driver",
                "ACTIVE"));
    when(policy.integer(PolicyKey.PICKUP_WAIT_EXTEND_MIN)).thenReturn(5);
    when(policy.integer(PolicyKey.PICKUP_WAIT_EXTEND_LIMIT)).thenReturn(1);
    when(policy.integer(PolicyKey.PAX_PREPAY_NO_SHOW_THRESHOLD)).thenReturn(2);
    when(waits.findPassengerAppUserId(BOOKING)).thenReturn(Optional.of(PASSENGER_APP_USER));
    when(reliability.currentPeriod()).thenReturn(java.time.LocalDate.of(2026, 8, 1));
    when(reliability.counter(anyLong(), any(), any())).thenReturn(new MonthlyCounterEntity());
    return new PickupWaitServiceImpl(
        current,
        identityFacade,
        waits,
        passengerStates,
        routing,
        policy,
        reliability,
        notifications,
        penalties,
        events,
        new SimpleMeterRegistry(),
        Clock.fixed(now, ZoneOffset.UTC));
  }

  private PickupWaitEntity newWait() {
    return PickupWaitEntity.startedOnArrival(TRIP, BOOKING, ARRIVED, Duration.ofMinutes(5), "{}");
  }

  private void driverOwnsTrip() {
    when(waits.isTripOwnedByDriverAppUser(TRIP, DRIVER_APP_USER)).thenReturn(true);
    when(waits.existsForTripAndBooking(TRIP, BOOKING)).thenReturn(true);
  }

  /**
   * The rule that makes the clock worth anything. A release a minute early is a fee taken off
   * somebody who was walking towards the car.
   */
  @Test
  void aDriverCannotReleaseTheSeatBeforeTheWaitHasRunOut() {
    var service = serviceAt(DEADLINE.minusSeconds(1));
    driverOwnsTrip();
    when(waits.findByBookingId(BOOKING)).thenReturn(Optional.of(newWait()));

    assertThatThrownBy(() -> service.releaseSeat(TRIP, BOOKING))
        .isInstanceOf(GateConflictException.class)
        .hasMessageContaining("has not run out");
    verify(routing, never()).releaseSeats(anyLong(), anyInt());
    verify(events, never()).publish(any());
  }

  /** 05-12: at the deadline the seat goes back, she is marked NO_SHOW, and slice 06 is told. */
  @Test
  void atTheDeadlineTheSeatIsReleasedAndTheNoShowIsRecordedAndPublished() {
    var service = serviceAt(DEADLINE);
    driverOwnsTrip();
    when(waits.findByBookingId(BOOKING)).thenReturn(Optional.of(newWait()));
    when(waits.findBookingSeatHold(BOOKING)).thenReturn(Optional.of(seatHold(55L, 2)));

    var response = service.releaseSeat(TRIP, BOOKING);

    assertThat(response.resolution()).isEqualTo(PickupWaitEntity.RESOLUTION_NO_SHOW);
    verify(routing).releaseSeats(55L, 2);
    verify(passengerStates).updateStatus(TRIP, BOOKING, PassengerTripStatus.NO_SHOW);
    verify(reliability)
        .record(
            org.mockito.ArgumentMatchers.eq(PASSENGER_APP_USER),
            any(),
            org.mockito.ArgumentMatchers.eq(ReliabilityEventType.NO_SHOW),
            any(),
            any(),
            any());

    ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
    verify(events).publish(event.capture());
    assertThat(event.getValue().eventType()).isEqualTo("booking.noshow");
    // The payload carries the deadline it was judged against, so slice 06 prices from the same
    // facts the release used rather than re-deriving them.
    assertThat(event.getValue().payloadJson()).contains(DEADLINE.toString());
  }

  /**
   * 05-10: there is no way to start a wait by asking, so acting on one that never started is 404.
   */
  @Test
  void thereIsNoWaitToActOnUntilGpsArrivalStartedOne() {
    var service = serviceAt(DEADLINE);
    driverOwnsTrip();
    when(waits.findByBookingId(BOOKING)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.releaseSeat(TRIP, BOOKING))
        .hasMessageContaining("has not been detected at the pickup point");
  }

  @Test
  void aDriverCannotTouchAWaitOnATripThatIsNotHis() {
    var service = serviceAt(DEADLINE);
    when(waits.isTripOwnedByDriverAppUser(TRIP, DRIVER_APP_USER)).thenReturn(false);

    assertThatThrownBy(() -> service.releaseSeat(TRIP, BOOKING))
        .isInstanceOf(AccessDeniedException.class);
    verify(routing, never()).releaseSeats(anyLong(), anyInt());
  }

  @Test
  void aPassengerCannotReadSomebodyElsesPickupWindow() {
    var service = serviceAt(ARRIVED);
    when(waits.isBookingOwnedByPassengerAppUser(BOOKING, DRIVER_APP_USER)).thenReturn(false);

    assertThatThrownBy(() -> service.passengerWindow(BOOKING))
        .isInstanceOf(AccessDeniedException.class);
  }

  /** The sweep applies the same rule the endpoint does, on rows the endpoint never touched. */
  @Test
  void theSweepReleasesExpiredWaits() {
    var service = serviceAt(DEADLINE.plusSeconds(30));
    when(waits.claimExpired(any(Instant.class), any(Pageable.class)))
        .thenReturn(List.of(newWait()));
    when(waits.findBookingSeatHold(BOOKING)).thenReturn(Optional.of(seatHold(55L, 1)));

    assertThat(service.sweepExpired(200)).isEqualTo(1);
    verify(routing).releaseSeats(55L, 1);
  }

  /**
   * She boarded between the sweep's read and this loop. A no-show recorded against a passenger
   * already in the car is indefensible, so the resolved row is skipped.
   */
  @Test
  void theSweepSkipsAWaitResolvedSinceItWasClaimed() {
    var service = serviceAt(DEADLINE.plusSeconds(30));
    var boarded = newWait();
    boarded.resolve(PickupWaitEntity.RESOLUTION_BOARDED, DEADLINE.minusSeconds(10));
    when(waits.claimExpired(any(Instant.class), any(Pageable.class))).thenReturn(List.of(boarded));

    assertThat(service.sweepExpired(200)).isZero();
    verify(routing, never()).releaseSeats(anyLong(), anyInt());
    verify(events, never()).publish(any());
  }

  private PickupWaitRepository.BookingSeatHoldRow seatHold(long occurrenceId, int seats) {
    return new PickupWaitRepository.BookingSeatHoldRow() {
      @Override
      public Long getRouteOccurrenceId() {
        return occurrenceId;
      }

      @Override
      public Integer getSeats() {
        return seats;
      }
    };
  }
}
