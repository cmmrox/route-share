package com.routeshare.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.common.security.CurrentUser;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.domain.AppUser;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.payment.dto.request.CashCollectionRequest;
import com.routeshare.payment.dto.request.PaymentLifecycleRequest;
import com.routeshare.payment.gateway.config.CommissionProperties;
import com.routeshare.payment.gateway.impl.CashFallbackPaymentGateway;
import com.routeshare.payment.repository.FareLedgerRepository;
import com.routeshare.payment.repository.PaymentIntentRepository;
import com.routeshare.payment.repository.PaymentIntentRepository.PaymentIntentView;
import com.routeshare.payment.repository.PaymentMethodRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentLifecycleServiceTest {
  private final CurrentUserProvider current = org.mockito.Mockito.mock(CurrentUserProvider.class);
  private final IdentityFacade identityFacade = org.mockito.Mockito.mock(IdentityFacade.class);
  private final BookingFacade bookingFacade = org.mockito.Mockito.mock(BookingFacade.class);
  private final PaymentIntentRepository paymentIntents =
      org.mockito.Mockito.mock(PaymentIntentRepository.class);
  private final FareLedgerRepository fareLedger =
      org.mockito.Mockito.mock(FareLedgerRepository.class);
  private final PaymentMethodRepository paymentMethods =
      org.mockito.Mockito.mock(PaymentMethodRepository.class);
  private final PaymentServiceImpl service =
      new PaymentServiceImpl(
          current,
          identityFacade,
          bookingFacade,
          paymentIntents,
          fareLedger,
          new CashFallbackPaymentGateway(),
          new CommissionProperties(new BigDecimal("0.10")),
          org.mockito.Mockito.mock(com.routeshare.pricing.facade.PricingFacade.class),
          paymentMethods,
          (action, key, limit, window) -> {},
          new com.routeshare.common.ratelimit.RateLimitProperties(true, null, null, null, null),
          org.mockito.Mockito.mock(com.routeshare.notification.facade.NotificationFacade.class));

  @BeforeEach
  void setUp() {
    var user =
        new CurrentUser("driver-sub", "driver@example.test", null, "Driver", Set.of("DRIVER"));
    var appUser =
        new AppUser(
            7L, UUID.randomUUID(), "driver-sub", "driver@example.test", null, "Driver", "ACTIVE");
    when(current.requireCurrentUser()).thenReturn(user);
    when(identityFacade.upsertFromToken(user)).thenReturn(appUser);
  }

  @Test
  void capturesRequiresCaptureIntentAndWritesLedger() {
    var view =
        new PaymentIntentView(
            11L, 99L, "MOCK", "mock_ref", "CAPTURED", new BigDecimal("1200.00"), "LKR");
    when(paymentIntents.transitionStatus(11L, "REQUIRES_CAPTURE", "CAPTURED"))
        .thenReturn(Optional.of(view));

    var response = service.capture(11L, new PaymentLifecycleRequest("Passenger onboarded"));

    assertThat(response).containsEntry("status", "CAPTURED");
    verify(fareLedger)
        .recordPaymentLifecycleIfAbsent(99L, "PAYMENT_CAPTURED", new BigDecimal("1200.00"), "LKR");
    // capture also records the platform commission split (10% of 1200.00) and net driver earning
    verify(fareLedger)
        .recordPaymentLifecycleIfAbsent(
            99L, "PLATFORM_COMMISSION", new BigDecimal("120.00"), "LKR");
    verify(fareLedger)
        .recordPaymentLifecycleIfAbsent(99L, "DRIVER_EARNING", new BigDecimal("1080.00"), "LKR");
  }

  @Test
  void voidsRequiresCaptureIntentAndWritesLedger() {
    var view =
        new PaymentIntentView(
            12L, 100L, "MOCK", "mock_ref2", "VOIDED", new BigDecimal("900.00"), "LKR");
    when(paymentIntents.transitionStatus(12L, "REQUIRES_CAPTURE", "VOIDED"))
        .thenReturn(Optional.of(view));

    var response = service.voidIntent(12L, new PaymentLifecycleRequest("Passenger cancelled"));

    assertThat(response).containsEntry("status", "VOIDED");
    verify(fareLedger)
        .recordPaymentLifecycleIfAbsent(100L, "PAYMENT_VOIDED", new BigDecimal("900.00"), "LKR");
  }

  @Test
  void refundsCapturedIntentAndWritesNegativeLedger() {
    var view =
        new PaymentIntentView(
            13L, 101L, "MOCK", "mock_ref3", "REFUNDED", new BigDecimal("750.00"), "LKR");
    when(paymentIntents.transitionStatus(13L, "CAPTURED", "REFUNDED"))
        .thenReturn(Optional.of(view));

    var response = service.refund(13L, new PaymentLifecycleRequest("Admin approved refund"));

    assertThat(response).containsEntry("status", "REFUNDED");
    verify(fareLedger)
        .recordPaymentLifecycleIfAbsent(101L, "PAYMENT_REFUNDED", new BigDecimal("-750.00"), "LKR");
  }

  @Test
  void recordsDriverCashCollectionAgainstBooking() {
    when(bookingFacade.findDriverOwnedBookingFare(55L, 7L))
        .thenReturn(Optional.of(new BigDecimal("500.00")));

    var response =
        service.recordCashCollected(
            55L, new CashCollectionRequest(new BigDecimal("500.00"), "Cash received"));

    assertThat(response).containsEntry("status", "CASH_COLLECTED");
    verify(fareLedger)
        .recordPaymentLifecycleIfAbsent(55L, "CASH_COLLECTED", new BigDecimal("500.00"), "LKR");
  }
}
