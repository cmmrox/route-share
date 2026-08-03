package com.routeshare.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.dto.request.BookingStatusTransitionRequest;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.BookingStatusHistoryRepository;
import com.routeshare.common.repository.IdempotencyKeyRepository;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.routing.facade.RouteReservation;
import com.routeshare.routing.facade.RoutingFacade;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingServiceTest {
  private final CurrentUserProvider current = org.mockito.Mockito.mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = org.mockito.Mockito.mock(IdentityFacade.class);
  private final BookingRepository bookings = org.mockito.Mockito.mock(BookingRepository.class);
  private final BookingStatusHistoryRepository statusHistory =
      org.mockito.Mockito.mock(BookingStatusHistoryRepository.class);
  private final RoutingFacade routingFacade = org.mockito.Mockito.mock(RoutingFacade.class);
  private final IdempotencyKeyRepository idempotencyKeys =
      org.mockito.Mockito.mock(IdempotencyKeyRepository.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final com.routeshare.notification.facade.NotificationFacade notifications =
      org.mockito.Mockito.mock(com.routeshare.notification.facade.NotificationFacade.class);
  private final com.routeshare.pricing.facade.PricingFacade pricing =
      org.mockito.Mockito.mock(com.routeshare.pricing.facade.PricingFacade.class);
  private final com.routeshare.payment.facade.PaymentFacade payments =
      org.mockito.Mockito.mock(com.routeshare.payment.facade.PaymentFacade.class);
  private final com.routeshare.trip.facade.TripLifecycleFacade tripLifecycle =
      org.mockito.Mockito.mock(com.routeshare.trip.facade.TripLifecycleFacade.class);
  private final com.routeshare.penalty.facade.PenaltyFacade penalties =
      org.mockito.Mockito.mock(com.routeshare.penalty.facade.PenaltyFacade.class);
  private final com.routeshare.booking.service.SeatHoldService seatHolds =
      org.mockito.Mockito.mock(com.routeshare.booking.service.SeatHoldService.class);
  private final com.routeshare.routing.service.PickupPointService pickupPoints =
      org.mockito.Mockito.mock(com.routeshare.routing.service.PickupPointService.class);
  private final com.routeshare.routing.service.EligibilityService eligibility =
      org.mockito.Mockito.mock(com.routeshare.routing.service.EligibilityService.class);
  private final com.routeshare.platform.service.PolicySettingService policy =
      org.mockito.Mockito.mock(com.routeshare.platform.service.PolicySettingService.class);
  private final java.time.Clock clock =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-08-02T08:00:00Z"), java.time.ZoneOffset.UTC);
  private final BookingServiceImpl service =
      new BookingServiceImpl(
          current,
          identityFacade,
          bookings,
          statusHistory,
          routingFacade,
          idempotencyKeys,
          notifications,
          objectMapper,
          pricing,
          payments,
          tripLifecycle,
          penalties,
          seatHolds,
          eligibility,
          pickupPoints,
          policy,
          org.mockito.Mockito.mock(com.routeshare.chat.facade.ChatFacade.class),
          org.mockito.Mockito.mock(com.routeshare.rewards.facade.RewardsFacade.class),
          clock);

  private static com.routeshare.pricing.domain.FareQuote quote(String passengerPays) {
    var amount = new java.math.BigDecimal(passengerPays);
    return new com.routeshare.pricing.domain.FareQuote(
        "LKR",
        new java.math.BigDecimal("5800.00"),
        new java.math.BigDecimal("5.8000"),
        new java.math.BigDecimal("50.00"),
        1,
        new java.math.BigDecimal("290.00"),
        new java.math.BigDecimal("92.00"),
        com.routeshare.pricing.domain.MatchDiscountTier.MID,
        new java.math.BigDecimal("8.00"),
        new java.math.BigDecimal("23.00"),
        amount,
        new java.math.BigDecimal("10.00"),
        new java.math.BigDecimal("27.00"),
        amount.subtract(new java.math.BigDecimal("27.00")),
        false,
        java.time.Instant.parse("2026-08-01T09:41:00Z"),
        "v1");
  }

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.when(
            policy.integer(com.routeshare.platform.domain.PolicyKey.MAX_OPEN_PASSENGER_REQUESTS))
        .thenReturn(2);
    org.mockito.Mockito.when(
            policy.integer(
                com.routeshare.platform.domain.PolicyKey.SCHEDULED_REQUEST_EXPIRY_MINUTES))
        .thenReturn(30);
    org.mockito.Mockito.when(seatHolds.approvalModeFor(org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(com.routeshare.routing.domain.ApprovalMode.INSTANT);
    org.mockito.Mockito.when(
            seatHolds.hold(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(java.util.List.of());
    org.mockito.Mockito.when(
            penalties.applyOutstandingDues(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(com.routeshare.penalty.dto.response.AppliedDuesResponse.empty());
    org.mockito.Mockito.when(
            pricing.quoteForMatch(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(quote("267.00"));
    var user =
        new CurrentUser(
            "subject", "passenger@example.test", null, "Passenger", Set.of("PASSENGER"));
    var appUser =
        new AppUser(
            7L,
            UUID.randomUUID(),
            "subject",
            "passenger@example.test",
            null,
            "Passenger",
            "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
  }

  @Test
  void booksAgainstOccurrenceAndStoresMatchedFractions() {
    var request = new BookingRequest(44L, 2, 6.90, 79.85, 6.95, 79.90, 0.25, 0.75, null, null);
    when(idempotencyKeys.reserveNew(anyString(), anyString(), anyString(), anyString()))
        .thenReturn("key-1");
    when(routingFacade.reserveSeatsAndReturnRouteLength(44L, 2))
        .thenReturn(java.util.Optional.of(new RouteReservation(12L, 44L, 91L, 10_000.0)));
    when(bookings.create(7L, request, 12L, new BigDecimal("267.00"))).thenReturn(99L);

    var response = service.book(request, "key-1");

    assertThat(response).containsEntry("bookingId", 99L);
    assertThat(response).containsEntry("status", "CONFIRMED");
    assertThat(response).containsEntry("routeOccurrenceId", 44L);
    // The stored estimate mirrors what the quote said the passenger pays.
    verify(bookings).create(7L, request, 12L, new BigDecimal("267.00"));
  }

  @Test
  void recordsInitialBookingStatusHistoryWhenBookingIsCreated() {
    var request = new BookingRequest(44L, 1, 6.90, 79.85, 6.95, 79.90, 0.25, 0.75, null, null);
    when(idempotencyKeys.reserveNew(anyString(), anyString(), anyString(), anyString()))
        .thenReturn("key-2");
    when(routingFacade.reserveSeatsAndReturnRouteLength(44L, 1))
        .thenReturn(java.util.Optional.of(new RouteReservation(12L, 44L, 91L, 8_000.0)));
    when(bookings.create(7L, request, 12L, new BigDecimal("267.00"))).thenReturn(100L);

    service.book(request, "key-2");

    verify(statusHistory)
        .recordInitialStatus(
            100L, "CONFIRMED", 7L, "Booking confirmed after occurrence seat reservation");
  }

  @Test
  void returnsStoredBookingResponseForDuplicateIdempotencyKeyWithoutCreatingAnotherBooking() {
    var request = new BookingRequest(44L, 1, 6.90, 79.85, 6.95, 79.90, 0.25, 0.75, null, null);
    when(idempotencyKeys.findActive("key-duplicate", "subject", "booking:create"))
        .thenReturn(
            java.util.Optional.of(
                storedResponse(
                    requestHash(request),
                    "{\"bookingId\":100,\"status\":\"CONFIRMED\",\"routeOccurrenceId\":44,\"fareEstimate\":671.00}",
                    200)));

    var response = service.book(request, "key-duplicate");

    assertThat(response).containsEntry("status", "CONFIRMED");
    assertThat(response).containsEntry("routeOccurrenceId", 44);
    verify(routingFacade, never())
        .reserveSeatsAndReturnRouteLength(any(Long.class), any(Integer.class));
    verify(bookings, never())
        .create(any(Long.class), any(BookingRequest.class), any(Long.class), any(BigDecimal.class));
  }

  @Test
  void storesSuccessfulBookingResponseAgainstIdempotencyKey() {
    var request = new BookingRequest(44L, 1, 6.90, 79.85, 6.95, 79.90, 0.25, 0.75, null, null);
    when(idempotencyKeys.reserveNew("key-store", "subject", "booking:create", requestHash(request)))
        .thenReturn("key-store");
    when(routingFacade.reserveSeatsAndReturnRouteLength(44L, 1))
        .thenReturn(java.util.Optional.of(new RouteReservation(12L, 44L, 91L, 8_000.0)));
    when(bookings.create(7L, request, 12L, new BigDecimal("267.00"))).thenReturn(100L);

    service.book(request, "key-store");

    verify(idempotencyKeys)
        .storeResponse(
            org.mockito.Mockito.eq("key-store"),
            org.mockito.Mockito.contains("\"bookingId\":100"),
            org.mockito.Mockito.eq(200));
  }

  @Test
  void cancelsConfirmedBookingAndRecordsStatusHistory() {
    var request = new BookingStatusTransitionRequest("CANCELLED", "Passenger changed plans");
    when(bookings.findStatusForUpdateByIdAndPassengerAppUserId(100L, 7L))
        .thenReturn(java.util.Optional.of("CONFIRMED"));
    when(bookings.updateStatus(100L, "CANCELLED")).thenReturn(1);

    var response = service.transition(100L, request);

    assertThat(response).containsEntry("bookingId", 100L);
    assertThat(response).containsEntry("status", "CANCELLED");
    verify(statusHistory)
        .recordTransition(100L, "CONFIRMED", "CANCELLED", 7L, "Passenger changed plans");
  }

  @Test
  void rejectsInvalidBookingStatusTransitionWithoutWritingHistory() {
    var request = new BookingStatusTransitionRequest("COMPLETED", "Should not jump");
    when(bookings.findStatusForUpdateByIdAndPassengerAppUserId(100L, 7L))
        .thenReturn(java.util.Optional.of("CANCELLED"));

    assertThatThrownBy(() -> service.transition(100L, request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid booking transition");

    verify(bookings, never()).updateStatus(any(Long.class), anyString());
    verify(statusHistory, never())
        .recordTransition(any(Long.class), anyString(), anyString(), any(Long.class), anyString());
  }

  private IdempotencyKeyRepository.StoredResponse storedResponse(
      String requestHash, String responseBody, int statusCode) {
    return new IdempotencyKeyRepository.StoredResponse() {
      @Override
      public String getRequestHash() {
        return requestHash;
      }

      @Override
      public String getResponseBody() {
        return responseBody;
      }

      @Override
      public Integer getStatusCode() {
        return statusCode;
      }
    };
  }

  private String requestHash(BookingRequest request) {
    try {
      var canonicalJson = objectMapper.writeValueAsString(request);
      var digest = MessageDigest.getInstance("SHA-256");
      var hash = digest.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(hash);
    } catch (JsonProcessingException | NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
