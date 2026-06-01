package com.routeshare.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.payment.dto.request.PaymentIntentRequest;
import com.routeshare.payment.repository.PaymentIntentRepository;
import com.routeshare.payment.repository.PaymentIntentRepository.PaymentIntentView;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class PaymentServiceTest {
  private final CurrentUserProvider current = org.mockito.Mockito.mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = org.mockito.Mockito.mock(IdentityFacade.class);
  private final BookingFacade bookingFacade = org.mockito.Mockito.mock(BookingFacade.class);
  private final PaymentIntentRepository paymentIntents =
      org.mockito.Mockito.mock(PaymentIntentRepository.class);
  private final PaymentServiceImpl service =
      new PaymentServiceImpl(current, identityFacade, bookingFacade, paymentIntents);

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
  void derivesAmountAndCurrencyFromOwnedBooking() {
    var amount = new BigDecimal("1234.50");
    when(bookingFacade.findFareEstimateForPassengerBooking(99L, 7L))
        .thenReturn(Optional.of(amount));
    when(paymentIntents.findActiveByBookingId(99L)).thenReturn(Optional.empty());
    when(paymentIntents.create(
            org.mockito.ArgumentMatchers.eq(99L),
            anyString(),
            org.mockito.ArgumentMatchers.eq(amount),
            org.mockito.ArgumentMatchers.eq("LKR")))
        .thenReturn(
            new PaymentIntentView("MOCK", "mock_reference", "REQUIRES_CAPTURE", amount, "LKR"));

    var response = service.createIntent(new PaymentIntentRequest(99L));

    assertThat(response).containsEntry("amount", amount);
    assertThat(response).containsEntry("currency", "LKR");
    verify(paymentIntents)
        .create(
            org.mockito.ArgumentMatchers.eq(99L),
            anyString(),
            org.mockito.ArgumentMatchers.eq(amount),
            org.mockito.ArgumentMatchers.eq("LKR"));
  }

  @Test
  void deniesPaymentIntentForBookingNotOwnedByCurrentPassenger() {
    when(bookingFacade.findFareEstimateForPassengerBooking(99L, 7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.createIntent(new PaymentIntentRequest(99L)))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Booking does not belong");
  }
}
