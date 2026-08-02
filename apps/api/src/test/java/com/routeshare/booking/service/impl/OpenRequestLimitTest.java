package com.routeshare.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.service.SeatHoldService;
import com.routeshare.common.errors.GateConflictException;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * P11: two unanswered requests at once.
 *
 * <p>The third is refused rather than queued, because every open request holds a seat. A rider with
 * five requests out has taken five seats out of five cars while she decides, and the drivers whose
 * inventory that is have agreed to none of it.
 */
class OpenRequestLimitTest {
  private static final long PASSENGER = 100L;

  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final BookingRepository bookings = mock(BookingRepository.class);
  private final PolicySettingService policy = mock(PolicySettingService.class);
  private final SeatHoldService seatHolds = mock(SeatHoldService.class);
  private final com.routeshare.routing.facade.RoutingFacade routing =
      mock(com.routeshare.routing.facade.RoutingFacade.class);
  private final com.routeshare.common.repository.IdempotencyKeyRepository idempotencyKeys =
      mock(com.routeshare.common.repository.IdempotencyKeyRepository.class);

  private BookingServiceImpl service() {
    var user = new CurrentUser("pax-sub", "p@example.test", null, "Passenger", Set.of("PASSENGER"));
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user))
        .thenReturn(
            new AppUser(
                PASSENGER,
                UUID.randomUUID(),
                "pax-sub",
                "p@example.test",
                null,
                "Dinuka",
                "ACTIVE"));
    when(policy.integer(PolicyKey.MAX_OPEN_PASSENGER_REQUESTS)).thenReturn(2);
    when(idempotencyKeys.findActive(any(), any(), any())).thenReturn(java.util.Optional.empty());
    when(idempotencyKeys.reserveNew(any(), any(), any(), any())).thenReturn("reserved");
    return new BookingServiceImpl(
        current,
        identityFacade,
        bookings,
        mock(com.routeshare.booking.repository.BookingStatusHistoryRepository.class),
        routing,
        idempotencyKeys,
        mock(com.routeshare.notification.facade.NotificationFacade.class),
        new com.fasterxml.jackson.databind.ObjectMapper(),
        mock(com.routeshare.pricing.facade.PricingFacade.class),
        mock(com.routeshare.payment.facade.PaymentFacade.class),
        mock(com.routeshare.trip.facade.TripLifecycleFacade.class),
        mock(com.routeshare.penalty.facade.PenaltyFacade.class),
        seatHolds,
        mock(com.routeshare.routing.service.EligibilityService.class),
        mock(com.routeshare.routing.service.PickupPointService.class),
        policy,
        java.time.Clock.systemUTC());
  }

  private com.routeshare.booking.dto.request.BookingRequest request() {
    return new com.routeshare.booking.dto.request.BookingRequest(
        44L, 1, 6.90, 79.85, 6.95, 79.90, 0.25, 0.75, null, null);
  }

  @Test
  @DisplayName("07-8: a third open request is refused with TOO_MANY_OPEN_REQUESTS")
  void thirdRequestIsRefused() {
    when(bookings.countOpenRequests(PASSENGER)).thenReturn(2);

    assertThatThrownBy(() -> service().book(request(), "idem-key"))
        .isInstanceOf(GateConflictException.class)
        .hasMessageContaining("waiting for a driver");

    // Refused before any seat is touched: a rejected request must not take inventory on its way
    // out.
    verify(seatHolds, never()).hold(anyLong(), anyLong(), any(), anyInt());
    verify(routing, never()).reserveSeatsAndReturnRouteLength(anyLong(), anyInt());
  }

  @Test
  @DisplayName("The refusal names the code the client maps to copy")
  void refusalCarriesTheCode() {
    when(bookings.countOpenRequests(PASSENGER)).thenReturn(5);
    try {
      service().book(request(), "idem-key");
      throw new AssertionError("expected a refusal");
    } catch (GateConflictException refused) {
      assertThat(refused.code()).isEqualTo("TOO_MANY_OPEN_REQUESTS");
    }
  }

  @Test
  @DisplayName("A second request is still allowed — the limit is two, not one")
  void secondRequestIsAllowedThrough() {
    when(bookings.countOpenRequests(PASSENGER)).thenReturn(1);
    when(routing.reserveSeatsAndReturnRouteLength(anyLong(), anyInt()))
        .thenReturn(java.util.Optional.empty());

    // Reaching the reservation and failing there is the proof: the guard let it past.
    assertThatThrownBy(() -> service().book(request(), "idem-key"))
        .isInstanceOf(IllegalStateException.class);
    verify(routing).reserveSeatsAndReturnRouteLength(anyLong(), anyInt());
  }
}
