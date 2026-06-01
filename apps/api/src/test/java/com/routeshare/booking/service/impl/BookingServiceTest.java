package com.routeshare.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.BookingStatusHistoryRepository;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.routing.facade.RouteReservation;
import com.routeshare.routing.facade.RoutingFacade;
import java.math.BigDecimal;
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
  private final BookingServiceImpl service =
      new BookingServiceImpl(current, identityFacade, bookings, statusHistory, routingFacade);

  @BeforeEach
  void setUp() {
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
    var request = new BookingRequest(44L, 2, 6.90, 79.85, 6.95, 79.90, 0.25, 0.75);
    when(routingFacade.reserveSeatsAndReturnRouteLength(44L, 2))
        .thenReturn(java.util.Optional.of(new RouteReservation(12L, 44L, 10_000.0)));
    when(bookings.create(7L, request, 12L, new BigDecimal("1540.00"))).thenReturn(99L);

    var response = service.book(request);

    assertThat(response).containsEntry("bookingId", 99L);
    assertThat(response).containsEntry("status", "CONFIRMED");
    assertThat(response).containsEntry("routeOccurrenceId", 44L);
    verify(bookings).create(7L, request, 12L, new BigDecimal("1540.00"));
  }

  @Test
  void recordsInitialBookingStatusHistoryWhenBookingIsCreated() {
    var request = new BookingRequest(44L, 1, 6.90, 79.85, 6.95, 79.90, 0.25, 0.75);
    when(routingFacade.reserveSeatsAndReturnRouteLength(44L, 1))
        .thenReturn(java.util.Optional.of(new RouteReservation(12L, 44L, 8_000.0)));
    when(bookings.create(7L, request, 12L, new BigDecimal("671.00"))).thenReturn(100L);

    service.book(request);

    verify(statusHistory)
        .recordInitialStatus(
            100L, "CONFIRMED", 7L, "Booking confirmed after occurrence seat reservation");
  }
}
