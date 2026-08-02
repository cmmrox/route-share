package com.routeshare.trip.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.common.event.DomainEvent;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.trip.entity.DriverLateGraceEntity;
import com.routeshare.trip.repository.DriverLateGraceRepository;
import java.math.BigDecimal;
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

/**
 * The third clock, and the endpoint both P26 and P34 read.
 *
 * <p>05-14 is the case worth stating plainly: the start buffer and this grace are different clocks
 * with different owners and different consequences. A trip that departed exactly on time can still
 * be twenty minutes from her corner, and this is the clock that says so.
 */
class DriverLateGraceServiceImplTest {
  private static final Instant DEPARTS = Instant.parse("2026-08-02T09:00:00Z");
  private static final Instant PROMISED = Instant.parse("2026-08-02T09:30:00Z");
  private static final Instant GRACE_EXPIRES = PROMISED.plus(Duration.ofMinutes(10));
  private static final long BOOKING = 100L;
  private static final long PASSENGER_APP_USER = 12L;

  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final DriverLateGraceRepository graces = mock(DriverLateGraceRepository.class);
  private final PolicySettingService policy = mock(PolicySettingService.class);
  private final NotificationFacade notifications = mock(NotificationFacade.class);
  private final DomainEventPublisher events = mock(DomainEventPublisher.class);

  private DriverLateGraceServiceImpl serviceAt(Instant now) {
    var user = new CurrentUser("pax-sub", "p@example.test", null, "Passenger", Set.of("PASSENGER"));
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user))
        .thenReturn(
            new AppUser(
                PASSENGER_APP_USER,
                UUID.randomUUID(),
                "pax-sub",
                "p@example.test",
                null,
                "Passenger",
                "ACTIVE"));
    when(policy.integer(PolicyKey.DRIVER_LATE_GRACE_MIN)).thenReturn(10);
    when(policy.integer(PolicyKey.DRIVER_CANCEL_FREE_HOURS)).thenReturn(2);
    when(policy.decimal(PolicyKey.LATE_CANCEL_PENALTY_PCT)).thenReturn(new BigDecimal("50.00"));
    when(graces.isBookingOwnedByPassengerAppUser(BOOKING, PASSENGER_APP_USER)).thenReturn(true);
    return new DriverLateGraceServiceImpl(
        current,
        identityFacade,
        graces,
        policy,
        notifications,
        events,
        Clock.fixed(now, ZoneOffset.UTC),
        30.0);
  }

  private DriverLateGraceEntity grace() {
    return DriverLateGraceEntity.opening(BOOKING, PROMISED, Duration.ofMinutes(10));
  }

  private void bookingContext(String status) {
    when(graces.findCancellationContext(BOOKING))
        .thenReturn(
            Optional.of(
                new DriverLateGraceRepository.CancellationContextRow() {
                  @Override
                  public Instant getDepartsAt() {
                    return DEPARTS;
                  }

                  @Override
                  public String getBookingStatus() {
                    return status;
                  }

                  @Override
                  public Instant getPromisedPickupAt() {
                    return PROMISED;
                  }
                }));
  }

  /** The grace runs from her promised pickup, not from the trip's departure. P35, in one line. */
  @Test
  void theGraceRunsFromHerPromisedPickupNotFromDeparture() {
    assertThat(grace().getGraceExpiresAt()).isEqualTo(GRACE_EXPIRES);
    assertThat(grace().getGraceExpiresAt()).isNotEqualTo(DEPARTS.plus(Duration.ofMinutes(10)));
  }

  /** 05-13: promised + 11 min with no driver detected — the free cancel unlocks. */
  @Test
  void pastTheGraceWithNoArrivalTheFreeCancelUnlocksAndSheIsTold() {
    var service = serviceAt(GRACE_EXPIRES.plus(Duration.ofMinutes(1)));
    var grace = grace();
    when(graces.claimExpired(any(Instant.class), any(Pageable.class))).thenReturn(List.of(grace));
    when(graces.hasDriverArrived(BOOKING)).thenReturn(false);
    when(graces.findPassengerAppUserId(BOOKING)).thenReturn(Optional.of(PASSENGER_APP_USER));

    assertThat(service.sweepExpired(200)).isEqualTo(1);
    assertThat(grace.isUnlocked()).isTrue();
    verify(notifications)
        .notifyUser(
            org.mockito.ArgumentMatchers.eq(PASSENGER_APP_USER),
            org.mockito.ArgumentMatchers.eq("DRIVER_LATE"),
            any(),
            any(),
            any());

    ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
    verify(events).publish(event.capture());
    assertThat(event.getValue().eventType()).isEqualTo("booking.driver_late");
  }

  /**
   * He is at her door. The grace protects her from a driver who has not arrived, and a detected
   * arrival says he has — unlocking a free cancel here would hand it to a passenger whose driver is
   * waiting for her.
   */
  @Test
  void aDriverWhoHasArrivedDoesNotUnlockAFreeCancelEvenPastTheGrace() {
    var service = serviceAt(GRACE_EXPIRES.plus(Duration.ofMinutes(1)));
    var grace = grace();
    when(graces.claimExpired(any(Instant.class), any(Pageable.class))).thenReturn(List.of(grace));
    when(graces.hasDriverArrived(BOOKING)).thenReturn(true);

    assertThat(service.sweepExpired(200)).isZero();
    assertThat(grace.isUnlocked()).isFalse();
    assertThat(grace.getResolution()).isEqualTo(DriverLateGraceEntity.RESOLUTION_PICKED_UP);
    verify(events, never()).publish(any());
  }

  /** 05-13 / 05-15: cancellation-terms says free, why, and that nothing is recorded against her. */
  @Test
  void onceUnlockedTheTermsSayFreeAndNothingIsRecordedAgainstHer() {
    var service = serviceAt(GRACE_EXPIRES.plus(Duration.ofMinutes(2)));
    var grace = grace();
    grace.unlock(GRACE_EXPIRES);
    bookingContext("CONFIRMED");
    when(graces.findByBookingId(BOOKING)).thenReturn(Optional.of(grace));

    var terms = service.cancellationTerms(BOOKING);

    assertThat(terms.free()).isTrue();
    assertThat(terms.reasonCode()).isEqualTo(DriverLateGraceServiceImpl.REASON_DRIVER_LATE);
    assertThat(terms.recordedAgainstPassenger()).isFalse();
    assertThat(terms.penaltyPct()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  /** Well before departure the ordinary free window applies, with no grace involved. */
  @Test
  void wellBeforeDepartureCancellingIsFreeOnTheOrdinaryWindow() {
    var service = serviceAt(DEPARTS.minus(Duration.ofHours(5)));
    bookingContext("CONFIRMED");
    when(graces.findByBookingId(BOOKING)).thenReturn(Optional.of(grace()));

    var terms = service.cancellationTerms(BOOKING);

    assertThat(terms.free()).isTrue();
    assertThat(terms.reasonCode())
        .isEqualTo(DriverLateGraceServiceImpl.REASON_OUTSIDE_PENALTY_WINDOW);
  }

  /**
   * 05-14: close to departure and the grace has not expired — she is inside the penalty window even
   * though his start buffer may already have run out. Two clocks, two outcomes.
   */
  @Test
  void insideThePenaltyWindowAndBeforeTheGraceTheCancelIsNotFree() {
    var service = serviceAt(DEPARTS.minus(Duration.ofMinutes(30)));
    bookingContext("CONFIRMED");
    when(graces.findByBookingId(BOOKING)).thenReturn(Optional.of(grace()));

    var terms = service.cancellationTerms(BOOKING);

    assertThat(terms.free()).isFalse();
    assertThat(terms.reasonCode()).isEqualTo(DriverLateGraceServiceImpl.REASON_LATE_CANCELLATION);
    assertThat(terms.recordedAgainstPassenger()).isTrue();
    assertThat(terms.penaltyPct()).isEqualByComparingTo(new BigDecimal("50.00"));
    // The countdown to when it would become free, so P34 can show it rather than guess.
    assertThat(terms.secondsUntilFreeCancel()).isNotNull();
  }

  /** The promised time is derived server-side; a request cannot move the deadline. */
  @Test
  void openingTheGraceDerivesThePromisedTimeAndStampsItOnTheBooking() {
    var service = serviceAt(DEPARTS);
    when(graces.findByBookingId(BOOKING)).thenReturn(Optional.empty());
    when(graces.computePromisedPickupAt(BOOKING, 30.0)).thenReturn(Optional.of(PROMISED));

    service.openForBooking(BOOKING);

    verify(graces).stampPromisedPickupAt(BOOKING, PROMISED);
    verify(graces).save(any(DriverLateGraceEntity.class));
  }

  /**
   * No geometry behind the booking means no clock, rather than a clock with an invented deadline.
   */
  @Test
  void aBookingWithNoDerivablePromisedTimeGetsNoGrace() {
    var service = serviceAt(DEPARTS);
    when(graces.findByBookingId(BOOKING)).thenReturn(Optional.empty());
    when(graces.computePromisedPickupAt(anyLong(), any(Double.class))).thenReturn(Optional.empty());

    service.openForBooking(BOOKING);

    verify(graces, never()).save(any(DriverLateGraceEntity.class));
    verify(graces, never()).stampPromisedPickupAt(anyLong(), any());
  }

  /** A cancel taken after the unlock is recorded as free; one taken before is not. */
  @Test
  void cancellingRecordsWhetherTheGraceHadUnlocked() {
    var service = serviceAt(GRACE_EXPIRES.plusSeconds(60));
    var unlocked = grace();
    unlocked.unlock(GRACE_EXPIRES);
    when(graces.findByBookingId(BOOKING)).thenReturn(Optional.of(unlocked));

    service.resolveCancelled(BOOKING);

    assertThat(unlocked.getResolution()).isEqualTo(DriverLateGraceEntity.RESOLUTION_FREE_CANCELLED);
  }
}
