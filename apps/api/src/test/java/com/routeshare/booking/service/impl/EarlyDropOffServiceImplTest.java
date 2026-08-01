package com.routeshare.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.dto.request.EarlyDropOffRequest;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class EarlyDropOffServiceImplTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = mock(IdentityFacade.class);
  private final BookingRepository bookings = mock(BookingRepository.class);
  private final PaymentService payments = mock(PaymentService.class);
  private final com.routeshare.pricing.facade.PricingFacade pricing =
      mock(com.routeshare.pricing.facade.PricingFacade.class);
  private final EarlyDropOffServiceImpl service =
      new EarlyDropOffServiceImpl(current, identityFacade, bookings, payments, pricing);

  @BeforeEach
  void setUp() {
    var user = new CurrentUser("sub", "p@test", null, "P", Set.of("PASSENGER"));
    var appUser = new AppUser(5L, UUID.randomUUID(), "sub", "p@test", null, "P", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
    when(pricing.repriceForActualDistance(
            org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            inv -> {
              // The repriced fare follows the distance actually travelled, at the booked rate.
              java.math.BigDecimal meters = inv.getArgument(1);
              java.math.BigDecimal pays =
                  meters
                      .divide(java.math.BigDecimal.valueOf(1000), 4, java.math.RoundingMode.HALF_UP)
                      .multiply(new java.math.BigDecimal("50"))
                      .setScale(0, java.math.RoundingMode.HALF_UP)
                      .setScale(2, java.math.RoundingMode.UNNECESSARY);
              return new com.routeshare.pricing.domain.FareQuote(
                  "LKR",
                  meters,
                  meters.divide(
                      java.math.BigDecimal.valueOf(1000), 4, java.math.RoundingMode.HALF_UP),
                  new java.math.BigDecimal("50.00"),
                  1,
                  pays,
                  new java.math.BigDecimal("92.00"),
                  com.routeshare.pricing.domain.MatchDiscountTier.MID,
                  java.math.BigDecimal.ZERO,
                  java.math.BigDecimal.ZERO,
                  pays,
                  new java.math.BigDecimal("10.00"),
                  java.math.BigDecimal.ZERO,
                  pays,
                  false,
                  java.time.Instant.parse("2026-08-01T09:41:00Z"),
                  "v1");
            });
  }

  private static BookingRepository.EarlyDropOffContext ctx(
      double exit, double routeLen, double pickup, double dropoff, int seats, String status) {
    return new BookingRepository.EarlyDropOffContext() {
      @Override
      public Double getExitFraction() {
        return exit;
      }

      @Override
      public BigDecimal getRouteLengthM() {
        return BigDecimal.valueOf(routeLen);
      }

      @Override
      public BigDecimal getPickupFraction() {
        return BigDecimal.valueOf(pickup);
      }

      @Override
      public BigDecimal getDropoffFraction() {
        return BigDecimal.valueOf(dropoff);
      }

      @Override
      public Integer getSeats() {
        return seats;
      }

      @Override
      public String getStatus() {
        return status;
      }
    };
  }

  @Test
  void finalizesFareByActualDistanceAndCaptures() {
    // Booked 0.2 -> 0.8 of a 10km route; passenger exits at 0.5 => 3km traveled.
    when(bookings.findEarlyDropOffContext(9L, 5L, 6.9, 79.8))
        .thenReturn(Optional.of(ctx(0.5, 10_000, 0.2, 0.8, 1, "CONFIRMED")));
    when(payments.finalizeBookingFare(eq(9L), any(BigDecimal.class)))
        .thenReturn(Map.of("captured", true));

    var res = service.finalizeEarlyDropOff(9L, new EarlyDropOffRequest(6.9, 79.8));

    assertThat(res.traveledMeters()).isEqualTo(3_000L);
    assertThat(res.captured()).isTrue();
    assertThat(res.finalFare()).isPositive();
    verify(bookings).updateDropoffFraction(eq(9L), any(BigDecimal.class));
    verify(payments).finalizeBookingFare(eq(9L), eq(res.finalFare()));
  }

  @Test
  void clampsExitBeyondDropoffToBookedDistance() {
    when(bookings.findEarlyDropOffContext(9L, 5L, 6.9, 79.8))
        .thenReturn(Optional.of(ctx(0.95, 10_000, 0.2, 0.8, 1, "CONFIRMED")));
    when(payments.finalizeBookingFare(eq(9L), any(BigDecimal.class)))
        .thenReturn(Map.of("captured", false));

    var res = service.finalizeEarlyDropOff(9L, new EarlyDropOffRequest(6.9, 79.8));

    // Clamped to dropoff (0.8): (0.8 - 0.2) * 10km = 6km.
    assertThat(res.traveledMeters()).isEqualTo(6_000L);
  }

  @Test
  void deniedWhenBookingNotOwned() {
    when(bookings.findEarlyDropOffContext(9L, 5L, 6.9, 79.8)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.finalizeEarlyDropOff(9L, new EarlyDropOffRequest(6.9, 79.8)))
        .isInstanceOf(AccessDeniedException.class);
    verify(payments, never()).finalizeBookingFare(any(Long.class), any());
  }

  @Test
  void rejectsNonActiveBooking() {
    when(bookings.findEarlyDropOffContext(9L, 5L, 6.9, 79.8))
        .thenReturn(Optional.of(ctx(0.5, 10_000, 0.2, 0.8, 1, "COMPLETED")));
    assertThatThrownBy(() -> service.finalizeEarlyDropOff(9L, new EarlyDropOffRequest(6.9, 79.8)))
        .isInstanceOf(IllegalStateException.class);
  }
}
