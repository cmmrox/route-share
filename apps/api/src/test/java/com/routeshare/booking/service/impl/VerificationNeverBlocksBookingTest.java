package com.routeshare.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.BookingStatusHistoryRepository;
import com.routeshare.booking.service.SeatHoldService;
import com.routeshare.common.errors.GateCodes;
import com.routeshare.common.errors.GateDeniedException;
import com.routeshare.common.repository.IdempotencyKeyRepository;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.payment.facade.PaymentFacade;
import com.routeshare.penalty.dto.response.AppliedDuesResponse;
import com.routeshare.penalty.facade.PenaltyFacade;
import com.routeshare.platform.domain.PolicyKey;
import com.routeshare.platform.service.PolicySettingService;
import com.routeshare.pricing.facade.PricingFacade;
import com.routeshare.routing.domain.ApprovalMode;
import com.routeshare.routing.facade.RouteReservation;
import com.routeshare.routing.facade.RoutingFacade;
import com.routeshare.routing.service.EligibilityService;
import com.routeshare.trip.facade.TripLifecycleFacade;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * P31a is a product promise, not a default: "Book, pay and ride as normal."
 *
 * <p>A rider at level {@code NONE} completes an ordinary booking, and the only thing that can ever
 * refuse her is a driver who asked for verification on that trip. This test exists so that stops
 * being true loudly rather than quietly — the failure mode it guards against is somebody reading
 * "verification" as "gate" and adding one check.
 */
class VerificationNeverBlocksBookingTest {

  private static final long APP_USER = 42L;
  private static final long OCCURRENCE = 77L;

  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identity = mock(IdentityFacade.class);
  private final BookingRepository bookings = mock(BookingRepository.class);
  private final RoutingFacade routing = mock(RoutingFacade.class);
  private final IdempotencyKeyRepository idempotencyKeys = mock(IdempotencyKeyRepository.class);
  private final PricingFacade pricing = mock(PricingFacade.class);
  private final PaymentFacade payments = mock(PaymentFacade.class);
  private final SeatHoldService seatHolds = mock(SeatHoldService.class);
  private final PenaltyFacade penalties = mock(PenaltyFacade.class);
  private final EligibilityService eligibility = mock(EligibilityService.class);
  private final com.routeshare.routing.service.PickupPointService pickupPoints =
      mock(com.routeshare.routing.service.PickupPointService.class);
  private final PolicySettingService policy = mock(PolicySettingService.class);

  private final BookingServiceImpl service =
      new BookingServiceImpl(
          current,
          identity,
          bookings,
          mock(BookingStatusHistoryRepository.class),
          routing,
          idempotencyKeys,
          mock(NotificationFacade.class),
          new com.fasterxml.jackson.databind.ObjectMapper(),
          pricing,
          payments,
          mock(TripLifecycleFacade.class),
          penalties,
          seatHolds,
          eligibility,
          pickupPoints,
          policy,
          mock(com.routeshare.chat.facade.ChatFacade.class),
          java.time.Clock.systemUTC());

  @BeforeEach
  void anUnverifiedRiderBookingAnOrdinaryTrip() {
    var token = new CurrentUser("sub", "d@example.test", "+94770000000", "Dinuka", Set.of());
    when(current.requireCurrentUser()).thenReturn(token);
    when(identity.upsertFromToken(token))
        .thenReturn(
            new AppUser(
                APP_USER,
                UUID.randomUUID(),
                "sub",
                "d@example.test",
                "+94770000000",
                "Dinuka",
                "ACTIVE"));
    when(idempotencyKeys.findActive(any(), any(), any())).thenReturn(Optional.empty());
    when(idempotencyKeys.reserveNew(any(), any(), any(), any())).thenReturn("reserved");
    when(policy.integer(PolicyKey.MAX_OPEN_PASSENGER_REQUESTS)).thenReturn(2);
    when(bookings.countOpenRequests(APP_USER)).thenReturn(0);
    when(routing.reserveSeatsAndReturnRouteLength(OCCURRENCE, 1))
        .thenReturn(Optional.of(new RouteReservation(11L, OCCURRENCE, 5L, 9_500d)));
    when(pricing.quoteForMatch(any(), anyLong(), any(), any(), anyInt())).thenReturn(quote());
    when(bookings.create(anyLong(), any(), anyLong(), any())).thenReturn(1234L);
    when(seatHolds.hold(anyLong(), anyLong(), any(), anyInt())).thenReturn(List.of());
    when(seatHolds.approvalModeFor(OCCURRENCE)).thenReturn(ApprovalMode.INSTANT);
    when(penalties.applyOutstandingDues(anyLong(), anyLong()))
        .thenReturn(AppliedDuesResponse.empty());
  }

  @Test
  @DisplayName("08-6: an unverified rider books an ordinary trip and is confirmed")
  void unverifiedRiderBooksAnOrdinaryTrip() {
    var response = service.book(request(), "idem-1");

    assertThat(response.get("status")).isEqualTo("CONFIRMED");
    assertThat(response.get("bookingId")).isEqualTo(1234L);
  }

  @Test
  @DisplayName("her card is authorised as normal — verification is not a payment condition either")
  void unverifiedRiderIsAuthorisedAsNormal() {
    service.book(request(), "idem-1");

    verify(payments).authorizeForBooking(anyLong(), any(), any());
  }

  @Test
  @DisplayName("the one thing that can refuse her is a trip whose driver asked for verification")
  void aVerifiedOnlyTripStillRefusesHer() {
    doThrow(
            new GateDeniedException(
                GateCodes.NOT_ELIGIBLE_VERIFIED_ONLY, "Verified riders only.", "/verify"))
        .when(eligibility)
        .requireEligible(APP_USER, OCCURRENCE);

    assertThatThrownBy(() -> service.book(request(), "idem-1"))
        .isInstanceOf(GateDeniedException.class);
  }

  @Test
  @DisplayName("an ineligible booking never touches inventory")
  void anIneligibleBookingNeverReservesASeat() {
    // Refusing after a seat had been reserved would leave the rollback holding the only copy of
    // the truth, and a leaked hold removes inventory permanently and silently.
    doThrow(new GateDeniedException(GateCodes.NOT_ELIGIBLE_WOMEN_ONLY, "Women only.", "/verify"))
        .when(eligibility)
        .requireEligible(APP_USER, OCCURRENCE);

    assertThatThrownBy(() -> service.book(request(), "idem-1"))
        .isInstanceOf(GateDeniedException.class);

    verify(routing, never()).reserveSeatsAndReturnRouteLength(anyLong(), anyInt());
    verify(seatHolds, never()).hold(anyLong(), anyLong(), any(), anyInt());
  }

  private static int anyInt() {
    return org.mockito.ArgumentMatchers.anyInt();
  }

  private static BookingRequest request() {
    return new BookingRequest(OCCURRENCE, 1, 6.90, 79.85, 6.95, 79.90, 0.25, 0.75, null, null);
  }

  private static com.routeshare.pricing.domain.FareQuote quote() {
    var pays = new BigDecimal("267.00");
    return new com.routeshare.pricing.domain.FareQuote(
        "LKR",
        new BigDecimal("5800.00"),
        new BigDecimal("5.8000"),
        new BigDecimal("50.00"),
        1,
        new BigDecimal("290.00"),
        new BigDecimal("92.00"),
        com.routeshare.pricing.domain.MatchDiscountTier.MID,
        new BigDecimal("8.00"),
        new BigDecimal("23.00"),
        pays,
        new BigDecimal("10.00"),
        new BigDecimal("27.00"),
        pays.subtract(new BigDecimal("27.00")),
        false,
        java.time.Instant.parse("2026-08-01T09:41:00Z"),
        "v1");
  }
}
