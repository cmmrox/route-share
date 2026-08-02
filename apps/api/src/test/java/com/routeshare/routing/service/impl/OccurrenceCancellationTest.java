package com.routeshare.routing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.common.event.DomainEventPublisher;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.payment.facade.PaymentFacade;
import com.routeshare.penalty.facade.PenaltyFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.routing.dto.request.OccurrenceCancellationRequest;
import com.routeshare.routing.repository.RouteOccurrenceCancellationRepository;
import com.routeshare.routing.repository.RouteOccurrenceRepository;
import com.routeshare.routing.repository.RouteOccurrenceSeatRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D30/D31: which window a driver's cancellation falls in, and what follows.
 *
 * <p>The window is decided here and the fee is priced by slice 06. These cases hold that seam: the
 * free side must never reach the penalty engine at all, and the penalised side must reach it
 * <em>before</em> the bookings are closed — pricing a stranded trip after stranding it would value
 * it at nothing.
 */
class OccurrenceCancellationTest {
  private static final long OCCURRENCE = 55L;
  private static final long TRIP = 77L;
  private static final long DRIVER = 200L;
  private static final Instant NOW = Instant.parse("2026-08-02T09:00:00Z");

  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final RouteOccurrenceRepository occurrences = mock(RouteOccurrenceRepository.class);
  private final RouteOccurrenceSeatRepository seats = mock(RouteOccurrenceSeatRepository.class);
  private final RouteOccurrenceCancellationRepository cancellations =
      mock(RouteOccurrenceCancellationRepository.class);
  private final PolicySettingService policy = mock(PolicySettingService.class);
  private final PenaltyFacade penalties = mock(PenaltyFacade.class);
  private final PaymentFacade payments = mock(PaymentFacade.class);
  private final BookingFacade bookings = mock(BookingFacade.class);
  private final NotificationFacade notifications = mock(NotificationFacade.class);
  private final DomainEventPublisher events = mock(DomainEventPublisher.class);

  private OccurrenceLifecycleServiceImpl serviceDepartingIn(Duration untilDeparture) {
    var user = new CurrentUser("drv-sub", "d@example.test", null, "Driver", Set.of("DRIVER"));
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user))
        .thenReturn(
            new AppUser(
                DRIVER, UUID.randomUUID(), "drv-sub", "d@example.test", null, "Priya", "ACTIVE"));
    when(occurrences.isOwnedByDriverAppUser(OCCURRENCE, DRIVER)).thenReturn(true);
    when(policy.integer(PolicyKey.DRIVER_CANCEL_FREE_HOURS)).thenReturn(12);
    when(policy.decimal(PolicyKey.LATE_CANCEL_PENALTY_PCT)).thenReturn(new BigDecimal("20"));
    when(bookings.findTripIdForOccurrence(OCCURRENCE)).thenReturn(Optional.of(TRIP));
    when(penalties.priceOccurrenceCancellation(anyLong(), any()))
        .thenReturn(
            new PenaltyFacade.PricedPenalty(
                money("429"), new BigDecimal("20"), money("86"), money("43"), money("43")));

    var context = mock(RouteOccurrenceSeatRepository.OccurrenceContextRow.class);
    when(context.getStatus()).thenReturn("PUBLISHED");
    when(context.getDepartsAt()).thenReturn(NOW.plus(untilDeparture));
    when(seats.findOccurrenceContext(OCCURRENCE)).thenReturn(Optional.of(context));

    var rider = mock(RouteOccurrenceRepository.AffectedRiderRow.class);
    when(rider.getBookingId()).thenReturn(1L);
    when(rider.getPassengerAppUserId()).thenReturn(100L);
    when(rider.getFirstName()).thenReturn("Dinuka");
    when(occurrences.findAffectedRiders(OCCURRENCE)).thenReturn(List.of(rider));
    when(bookings.cancelOpenBookingsForOccurrence(anyLong(), any(), anyLong()))
        .thenReturn(List.of(new BookingFacade.CancelledBooking(1L, 100L, "CONFIRMED")));

    return new OccurrenceLifecycleServiceImpl(
        current,
        identityFacade,
        occurrences,
        seats,
        cancellations,
        policy,
        penalties,
        payments,
        bookings,
        mock(com.routeshare.driver.facade.DriverFacade.class),
        notifications,
        events,
        new SimpleMeterRegistry(),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("07-11: cancelling 26 hours out is free and assesses nothing")
  void outsideTheWindowIsFree() {
    var service = serviceDepartingIn(Duration.ofHours(26));

    var result = service.cancel(OCCURRENCE, new OccurrenceCancellationRequest("UNWELL", null));

    assertThat(result.get("withinFreeWindow")).isEqualTo(true);
    verify(penalties, never()).assessDriverLateCancellation(anyLong());
  }

  @Test
  @DisplayName("07-12: cancelling 3 hours out is priced by slice 06 and every rider is told")
  void insideTheWindowIsPenalised() {
    var service = serviceDepartingIn(Duration.ofHours(3));

    var result =
        service.cancel(OCCURRENCE, new OccurrenceCancellationRequest("PLANS_CHANGED", "sorry"));

    assertThat(result.get("withinFreeWindow")).isEqualTo(false);
    verify(penalties).assessDriverLateCancellation(TRIP);
    verify(payments).voidForBooking(1L, "OCCURRENCE_CANCELLED");
    verify(notifications).notifyUser(anyLong(), any(), any(), any(), any());
    verify(cancellations).save(any());
  }

  @Test
  @DisplayName("The penalty is priced while the bookings are still open, never after")
  void penaltyIsAssessedBeforeTheBookingsAreClosed() {
    var service = serviceDepartingIn(Duration.ofHours(3));
    var order = org.mockito.Mockito.inOrder(penalties, bookings);

    service.cancel(OCCURRENCE, new OccurrenceCancellationRequest("VEHICLE_PROBLEM", null));

    order.verify(penalties).assessDriverLateCancellation(TRIP);
    order.verify(bookings).cancelOpenBookingsForOccurrence(anyLong(), any(), anyLong());
  }

  @Test
  @DisplayName("Exactly at the boundary, twelve hours out is still free")
  void theBoundaryItselfIsFree() {
    var service = serviceDepartingIn(Duration.ofHours(12));
    var terms = service.cancellationTerms(OCCURRENCE);

    assertThat(terms.withinFreeWindow()).isTrue();
    assertThat(terms.penaltyAmount()).isEqualByComparingTo(money("0"));
  }

  @Test
  @DisplayName("D30's terms name the fee, the riders' share and who is affected")
  void termsStateEveryFigureTheScreenNeeds() {
    var terms = serviceDepartingIn(Duration.ofHours(3)).cancellationTerms(OCCURRENCE);

    assertThat(terms.withinFreeWindow()).isFalse();
    assertThat(terms.penaltyAmount()).isEqualByComparingTo(money("86"));
    assertThat(terms.riderShare()).isEqualByComparingTo(money("43"));
    assertThat(terms.platformShare()).isEqualByComparingTo(money("43"));
    assertThat(terms.affectedRiderFirstNames()).containsExactly("Dinuka");
    assertThat(terms.reasonCodes()).contains("VEHICLE_PROBLEM", "UNWELL", "OTHER");
    assertThat(terms.hoursBeforeDeparture()).isEqualByComparingTo(new BigDecimal("3.00"));
  }

  @Test
  @DisplayName("A trip that has already gone reports zero hours out, never a negative")
  void departedTripsReportZeroHours() {
    var terms = serviceDepartingIn(Duration.ofHours(-5)).cancellationTerms(OCCURRENCE);
    assertThat(terms.hoursBeforeDeparture()).isEqualByComparingTo(new BigDecimal("0.00"));
  }

  private static BigDecimal money(String value) {
    return new BigDecimal(value).setScale(2);
  }
}
