package com.routeshare.payment.facade.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.payment.domain.CaptureOutcome;
import com.routeshare.payment.entity.PaymentAttemptEntity;
import com.routeshare.payment.entity.PaymentIntentEntity;
import com.routeshare.payment.entity.PaymentMethodEntity;
import com.routeshare.payment.gateway.PaymentGatewayPort;
import com.routeshare.payment.repository.FareLedgerRepository;
import com.routeshare.payment.repository.PaymentAttemptRepository;
import com.routeshare.payment.repository.PaymentIntentRepository;
import com.routeshare.payment.repository.PaymentMethodRepository;
import com.routeshare.pricing.facade.PricingFacade;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The promise this slice exists to keep: authorised at booking, captured at start, released if the
 * trip never happens — and each of those exactly once, however many times the button is pressed.
 */
class PaymentFacadeImplTest {
  private static final Instant NOW = Instant.parse("2026-08-02T09:41:00Z");
  private static final long BOOKING_ID = 42L;
  private static final long TRIP_ID = 77L;
  private static final BigDecimal FARE = new BigDecimal("267.00");

  private final PaymentIntentRepository intents = mock(PaymentIntentRepository.class);
  private final PaymentAttemptRepository attempts = mock(PaymentAttemptRepository.class);
  private final PaymentMethodRepository paymentMethods = mock(PaymentMethodRepository.class);
  private final FareLedgerRepository fareLedger = mock(FareLedgerRepository.class);
  private final BookingFacade bookings = mock(BookingFacade.class);
  private final PricingFacade pricing = mock(PricingFacade.class);
  private final PaymentGatewayPort gateway = mock(PaymentGatewayPort.class);
  private final NotificationFacade notifications = mock(NotificationFacade.class);

  private final PaymentFacadeImpl facade =
      new PaymentFacadeImpl(
          intents,
          attempts,
          paymentMethods,
          fareLedger,
          bookings,
          pricing,
          gateway,
          notifications,
          new SimpleMeterRegistry(),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @BeforeEach
  void setUp() {
    when(intents.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
    when(attempts.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(attempts.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
    when(bookings.findPassengerAppUserIdForBooking(anyLong())).thenReturn(Optional.of(5L));
    var method = mock(PaymentMethodEntity.class);
    when(method.getToken()).thenReturn("tok_123");
    when(paymentMethods.findById(7L)).thenReturn(Optional.of(method));
  }

  private static PaymentIntentEntity withId(PaymentIntentEntity intent) {
    if (intent.getId() == null) {
      intent.setId(1L);
    }
    return intent;
  }

  private PaymentIntentEntity authorizedIntent() {
    var intent = PaymentIntentEntity.pending(BOOKING_ID, FARE, "LKR", 7L);
    intent.setId(1L);
    intent.authorize("auth_ref", NOW);
    return intent;
  }

  private void gatewayApproves() {
    when(gateway.authorize(any()))
        .thenReturn(new PaymentGatewayPort.AuthorizationResult("auth_ref", "AUTHORIZED", true));
  }

  // ── booking authorises, and cash does not ────────────────────────────────────────────────────

  @Test
  void aCardBookingHoldsTheFareWithoutTakingIt() {
    gatewayApproves();

    facade.authorizeForBooking(BOOKING_ID, 7L, FARE);

    verify(gateway).authorize(any());
    verify(gateway, never()).capture(anyString(), any(), anyString());
    verify(bookings).recordPaymentState(BOOKING_ID, "CARD", "AUTHORIZED", null);
  }

  @Test
  void aCashBookingCreatesNoIntentAtAll() {
    facade.authorizeForBooking(BOOKING_ID, null, FARE);

    // A placeholder intent would be a lie the reconciliation job would later have to chase.
    verify(intents, never()).save(any());
    verify(gateway, never()).authorize(any());
    verify(bookings).recordPaymentState(BOOKING_ID, "CASH", "PENDING", null);
  }

  @Test
  void aDeclinedAuthorizationIsRecordedRatherThanThrown() {
    when(gateway.authorize(any()))
        .thenReturn(new PaymentGatewayPort.AuthorizationResult("ref", "DECLINED", false));

    facade.authorizeForBooking(BOOKING_ID, 7L, FARE);

    verify(bookings).recordPaymentState(BOOKING_ID, "CARD", "FAILED", null);
  }

  // ── trip start captures, once each ───────────────────────────────────────────────────────────

  @Test
  void startingTheTripChargesEveryConfirmedBooking() {
    when(bookings.findConfirmedBookingsForTrip(TRIP_ID))
        .thenReturn(
            List.of(
                new BookingFacade.BookingToCharge(BOOKING_ID, FARE),
                new BookingFacade.BookingToCharge(43L, FARE)));
    // A fresh intent per booking: each has its own row in reality, and sharing one instance here
    // would have the first capture mutate the second booking's state.
    when(intents.findLatestForBooking(anyLong()))
        .thenAnswer(inv -> Optional.of(authorizedIntent()));

    List<CaptureOutcome> outcomes = facade.captureForTripStart(TRIP_ID);

    assertThat(outcomes).hasSize(2);
    assertThat(outcomes).allMatch(outcome -> outcome.result() == CaptureOutcome.Result.CAPTURED);
    verify(gateway, times(2)).capture(anyString(), eq(FARE), anyString());
  }

  @Test
  void aRetriedStartChargesNothingFurther() {
    when(bookings.findConfirmedBookingsForTrip(TRIP_ID))
        .thenReturn(List.of(new BookingFacade.BookingToCharge(BOOKING_ID, FARE)));
    var captured = authorizedIntent();
    captured.capture(NOW);
    when(intents.findLatestForBooking(BOOKING_ID)).thenReturn(Optional.of(captured));

    List<CaptureOutcome> outcomes = facade.captureForTripStart(TRIP_ID);

    assertThat(outcomes.get(0).result()).isEqualTo(CaptureOutcome.Result.ALREADY_CAPTURED);
    verify(gateway, never()).capture(anyString(), any(), anyString());
  }

  @Test
  void aDuplicateCallIsStoppedByTheIdempotencyKeyEvenBeforeTheStatusCheck() {
    when(bookings.findConfirmedBookingsForTrip(TRIP_ID))
        .thenReturn(List.of(new BookingFacade.BookingToCharge(BOOKING_ID, FARE)));
    when(intents.findLatestForBooking(BOOKING_ID)).thenReturn(Optional.of(authorizedIntent()));
    when(attempts.findByIdempotencyKey("capture:booking:42"))
        .thenReturn(Optional.of(mock(PaymentAttemptEntity.class)));

    List<CaptureOutcome> outcomes = facade.captureForTripStart(TRIP_ID);

    // Two concurrent taps: the second finds the attempt row already written and never reaches the
    // provider. Exactly one capture is the whole point of the slice.
    verify(gateway, never()).capture(anyString(), any(), anyString());
    assertThat(outcomes.get(0).result()).isEqualTo(CaptureOutcome.Result.ALREADY_CAPTURED);
  }

  @Test
  void aDeclinedCardFlagsItsBookingWithoutStoppingTheTrip() {
    when(bookings.findConfirmedBookingsForTrip(TRIP_ID))
        .thenReturn(
            List.of(
                new BookingFacade.BookingToCharge(BOOKING_ID, FARE),
                new BookingFacade.BookingToCharge(43L, FARE)));
    when(intents.findLatestForBooking(anyLong()))
        .thenAnswer(inv -> Optional.of(authorizedIntent()));
    doThrow(new IllegalStateException("bank declined"))
        .when(gateway)
        .capture(anyString(), any(), anyString());

    List<CaptureOutcome> outcomes = facade.captureForTripStart(TRIP_ID);

    // The driver is at the wheel and the others are in the car; a refused bank flags that booking
    // and nothing else.
    assertThat(outcomes).hasSize(2);
    assertThat(outcomes).allMatch(outcome -> outcome.result() == CaptureOutcome.Result.FAILED);
    assertThat(outcomes.get(0).failureCode()).isEqualTo("GATEWAY_ERROR");
  }

  @Test
  void aCashBookingIsSkippedRatherThanFailed() {
    when(bookings.findConfirmedBookingsForTrip(TRIP_ID))
        .thenReturn(List.of(new BookingFacade.BookingToCharge(BOOKING_ID, FARE)));
    when(intents.findLatestForBooking(BOOKING_ID)).thenReturn(Optional.empty());

    List<CaptureOutcome> outcomes = facade.captureForTripStart(TRIP_ID);

    assertThat(outcomes.get(0).result()).isEqualTo(CaptureOutcome.Result.SKIPPED_CASH);
  }

  // ── nothing happened, nothing charged ────────────────────────────────────────────────────────

  @Test
  void cancellingBeforeTheStartReleasesTheHold() {
    when(intents.findLatestForBooking(BOOKING_ID)).thenReturn(Optional.of(authorizedIntent()));

    facade.voidForBooking(BOOKING_ID, "PASSENGER_CANCELLED");

    verify(gateway).voidAuthorization("auth_ref");
    verify(bookings).recordPaymentState(BOOKING_ID, null, "VOIDED", null);
  }

  @Test
  void voidingAnAlreadyCapturedPaymentIsRefusedNotAttempted() {
    var captured = authorizedIntent();
    captured.capture(NOW);
    when(intents.findLatestForBooking(BOOKING_ID)).thenReturn(Optional.of(captured));

    facade.voidForBooking(BOOKING_ID, "PASSENGER_CANCELLED");

    verify(gateway, never()).voidAuthorization(anyString());
  }

  @Test
  void cancellingACashBookingIsHarmless() {
    when(intents.findLatestForBooking(BOOKING_ID)).thenReturn(Optional.empty());

    facade.voidForBooking(BOOKING_ID, "PASSENGER_CANCELLED");

    verify(gateway, never()).voidAuthorization(anyString());
  }

  // ── early drop-off ───────────────────────────────────────────────────────────────────────────

  @Test
  void gettingOutEarlyBeforeCaptureSimplyChargesLess() {
    when(intents.findLatestForBooking(BOOKING_ID)).thenReturn(Optional.of(authorizedIntent()));

    facade.settleRepricedFare(BOOKING_ID, new BigDecimal("180.00"));

    verify(gateway).capture(anyString(), eq(new BigDecimal("180.00")), anyString());
  }

  @Test
  void gettingOutEarlyAfterCaptureRefundsTheDifference() {
    var captured = authorizedIntent();
    captured.capture(NOW);
    when(intents.findLatestForBooking(BOOKING_ID)).thenReturn(Optional.of(captured));

    facade.settleRepricedFare(BOOKING_ID, new BigDecimal("180.00"));

    verify(gateway).refund(anyString(), eq(new BigDecimal("87.00")), anyString());
  }
}
