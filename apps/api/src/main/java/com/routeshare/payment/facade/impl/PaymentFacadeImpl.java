package com.routeshare.payment.facade.impl;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.notification.facade.NotificationFacade;
import com.routeshare.payment.domain.CaptureOutcome;
import com.routeshare.payment.domain.PaymentIntentStatus;
import com.routeshare.payment.entity.PaymentAttemptEntity;
import com.routeshare.payment.entity.PaymentIntentEntity;
import com.routeshare.payment.facade.PaymentFacade;
import com.routeshare.payment.gateway.PaymentGatewayPort;
import com.routeshare.payment.repository.FareLedgerRepository;
import com.routeshare.payment.repository.PaymentAttemptRepository;
import com.routeshare.payment.repository.PaymentIntentRepository;
import com.routeshare.payment.repository.PaymentMethodRepository;
import com.routeshare.pricing.facade.PricingFacade;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Where the money actually moves.
 *
 * <p>Three rules run through every method here, and each of them exists because the alternative is
 * a specific, expensive failure:
 *
 * <ol>
 *   <li><b>Every gateway call is recorded before it is made.</b> A capture that times out has
 *       either happened or not; without the attempt row there is no way to tell, and a blind retry
 *       charges someone twice.
 *   <li><b>Idempotency keys are deterministic.</b> {@code capture:booking:42} is the same key on
 *       the first tap and the retry, and the database's unique index is what makes "exactly once"
 *       true rather than merely intended.
 *   <li><b>A failed capture never stops the trip.</b> The driver is at the wheel and the other
 *       passengers are in the car. The booking is flagged and operations are told; the trip runs.
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFacadeImpl implements PaymentFacade {
  private static final String CURRENCY = "LKR";
  private static final String CASH = "CASH";
  private static final String CARD = "CARD";
  private static final String CASH_REFERENCE_PREFIX = "cash_";

  private final PaymentIntentRepository intents;
  private final PaymentAttemptRepository attempts;
  private final PaymentMethodRepository paymentMethods;
  private final FareLedgerRepository fareLedger;
  private final BookingFacade bookings;
  private final PricingFacade pricing;
  private final PaymentGatewayPort gateway;
  private final NotificationFacade notifications;
  private final MeterRegistry meters;
  private final Clock clock;

  @Override
  @Transactional
  public void authorizeForBooking(long bookingId, Long paymentMethodId, BigDecimal amount) {
    if (paymentMethodId == null) {
      // Cash: no card, no hold, no intent. A placeholder row here would be a lie the reconciliation
      // job would later have to chase.
      bookings.recordPaymentState(bookingId, CASH, PaymentIntentStatus.PENDING.name(), null);
      return;
    }
    if (amount == null || amount.signum() <= 0) {
      throw new IllegalArgumentException("Cannot authorise a non-positive amount");
    }

    var method = paymentMethods.findById(paymentMethodId).orElse(null);
    if (method == null) {
      throw new IllegalArgumentException("Unknown payment method");
    }

    PaymentIntentEntity intent =
        intents.save(PaymentIntentEntity.pending(bookingId, amount, CURRENCY, paymentMethodId));
    String key = "authorize:booking:" + bookingId;

    attempt(
        intent,
        bookingId,
        "AUTHORIZE",
        key,
        amount,
        () -> {
          var result =
              gateway.authorize(
                  new PaymentGatewayPort.AuthorizeCommand(
                      bookingId, amount, CURRENCY, method.getToken()));
          if (!result.approved()) {
            throw new PaymentDeclinedException("PAYMENT_AUTHORIZATION_FAILED");
          }
          intent.authorize(result.providerReference(), clock.instant());
          intents.save(intent);
          fareLedger.recordPaymentLifecycleIfAbsent(
              bookingId, "PAYMENT_AUTHORIZED", amount, CURRENCY);
          bookings.recordPaymentState(bookingId, CARD, PaymentIntentStatus.AUTHORIZED.name(), null);
          return result.providerReference();
        },
        failureCode -> {
          intent.fail(failureCode, clock.instant());
          intents.save(intent);
          bookings.recordPaymentState(bookingId, CARD, PaymentIntentStatus.FAILED.name(), null);
        });
  }

  @Override
  @Transactional
  public List<CaptureOutcome> captureForTripStart(long tripId) {
    List<CaptureOutcome> outcomes = new ArrayList<>();
    for (var booking : bookings.findConfirmedBookingsForTrip(tripId)) {
      outcomes.add(captureOne(booking.bookingId(), booking.fare()));
    }
    return List.copyOf(outcomes);
  }

  @Override
  @Transactional
  public void voidForBooking(long bookingId, String reason) {
    Optional<PaymentIntentEntity> maybeIntent = intents.findLatestForBooking(bookingId);
    if (maybeIntent.isEmpty()) {
      // Cash, or a booking that never reached a card. Nothing is held, so nothing to release.
      bookings.recordPaymentState(bookingId, null, PaymentIntentStatus.VOIDED.name(), null);
      return;
    }
    PaymentIntentEntity intent = maybeIntent.get();
    PaymentIntentStatus status = PaymentIntentStatus.of(intent.getStatus());
    if (status.isSettled()) {
      // Already captured: releasing is not the remedy, refunding is — and that is a different
      // decision with a different owner (slice 06's penalties).
      log.warn("void requested on a settled payment bookingId={} status={}", bookingId, status);
      return;
    }
    if (status == PaymentIntentStatus.VOIDED) {
      return;
    }

    attempt(
        intent,
        bookingId,
        "VOID",
        "void:booking:" + bookingId,
        intent.getAmount(),
        () -> {
          if (isExternal(intent)) {
            gateway.voidAuthorization(intent.getProviderReference());
          }
          intent.voidAuthorization(clock.instant());
          intents.save(intent);
          fareLedger.recordPaymentLifecycleIfAbsent(
              bookingId, "PAYMENT_VOIDED", intent.getAmount(), CURRENCY);
          bookings.recordPaymentState(bookingId, null, PaymentIntentStatus.VOIDED.name(), null);
          meters.counter("routeshare_payment_voids_total", "reason", safe(reason)).increment();
          notify(
              bookingId,
              "PAYMENT_VOIDED",
              "Nothing was charged",
              "The hold on your card has been released. You have not been charged.");
          return intent.getProviderReference();
        },
        failureCode ->
            log.error(
                "void failed bookingId={} reason={} code={}", bookingId, reason, failureCode));
  }

  @Override
  @Transactional
  public void settleRepricedFare(long bookingId, BigDecimal finalAmount) {
    Optional<PaymentIntentEntity> maybeIntent = intents.findLatestForBooking(bookingId);
    if (maybeIntent.isEmpty()) {
      return; // cash: the driver collects the repriced amount in the car
    }
    PaymentIntentEntity intent = maybeIntent.get();
    PaymentIntentStatus status = PaymentIntentStatus.of(intent.getStatus());

    if (status.isAuthorizedNotCaptured()) {
      // Nothing taken yet: capture the lower figure and the passenger never sees the difference.
      intent.setAmount(finalAmount);
      intents.save(intent);
      captureOne(bookingId, finalAmount);
      return;
    }
    if (status == PaymentIntentStatus.CAPTURED) {
      BigDecimal difference = intent.getAmount().subtract(finalAmount);
      if (difference.signum() <= 0) {
        return;
      }
      attempt(
          intent,
          bookingId,
          "REFUND",
          "refund:booking:"
              + bookingId
              + ":amount:"
              + difference.stripTrailingZeros().toPlainString(),
          difference,
          () -> {
            if (isExternal(intent)) {
              gateway.refund(intent.getProviderReference(), difference, CURRENCY);
            }
            fareLedger.recordPaymentLifecycleIfAbsent(
                bookingId, "PAYMENT_REFUNDED", difference.negate(), CURRENCY);
            notify(
                bookingId,
                "PAYMENT_REFUNDED",
                "Part of your fare was refunded",
                "You got out early, so " + difference + " " + CURRENCY + " is on its way back.");
            return intent.getProviderReference();
          },
          failureCode -> log.error("refund failed bookingId={} code={}", bookingId, failureCode));
    }
  }

  @Override
  @Transactional
  public void recordCashCommissionOwed(long bookingId, BigDecimal fareCollected) {
    BigDecimal commission =
        pricing
            .quoteForBooking(bookingId)
            .map(quote -> quote.commissionAmount())
            .orElse(BigDecimal.ZERO);
    if (commission.signum() <= 0) {
      return;
    }
    // The driver has the passenger's cash in hand, so the platform's cut is owed rather than taken.
    // It nets from the next payout (boards D23 and D27).
    fareLedger.recordPaymentLifecycleIfAbsent(
        bookingId, "COMMISSION_OWED_CASH", commission, CURRENCY);
  }

  // ── internals ────────────────────────────────────────────────────────────────────────────────

  private CaptureOutcome captureOne(long bookingId, BigDecimal fare) {
    Optional<PaymentIntentEntity> maybeIntent = intents.findLatestForBooking(bookingId);
    if (maybeIntent.isEmpty()) {
      return CaptureOutcome.skippedCash(bookingId, fare);
    }
    PaymentIntentEntity intent = maybeIntent.get();
    PaymentIntentStatus status = PaymentIntentStatus.of(intent.getStatus());

    if (status.isSettled()) {
      // A retried start. The database's unique idempotency key would refuse the second attempt
      // anyway; answering here keeps the response honest about why nothing happened.
      return CaptureOutcome.alreadyCaptured(bookingId, intent.getAmount());
    }
    if (!status.isAuthorizedNotCaptured()) {
      return CaptureOutcome.failed(bookingId, intent.getAmount(), "NOT_AUTHORIZED");
    }

    BigDecimal amount = intent.getAmount();
    Timer.Sample sample = Timer.start(meters);
    var captured = new boolean[] {false};
    var failure = new String[] {null};

    attempt(
        intent,
        bookingId,
        "CAPTURE",
        "capture:booking:" + bookingId,
        amount,
        () -> {
          if (isExternal(intent)) {
            gateway.capture(intent.getProviderReference(), amount, CURRENCY);
          }
          intent.capture(clock.instant());
          intents.save(intent);
          fareLedger.recordPaymentLifecycleIfAbsent(
              bookingId, "PAYMENT_CAPTURED", amount, CURRENCY);
          bookings.recordPaymentState(
              bookingId, CARD, PaymentIntentStatus.CAPTURED.name(), clock.instant());
          captured[0] = true;
          notify(
              bookingId,
              "PAYMENT_CAPTURED",
              "Your fare was charged",
              "Your trip has started, so " + amount + " " + CURRENCY + " has been charged.");
          return intent.getProviderReference();
        },
        failureCode -> {
          failure[0] = failureCode;
          intent.fail(failureCode, clock.instant());
          intents.save(intent);
          fareLedger.recordPaymentLifecycleIfAbsent(bookingId, "PAYMENT_FAILED", amount, CURRENCY);
          bookings.recordPaymentState(bookingId, CARD, PaymentIntentStatus.FAILED.name(), null);
        });

    sample.stop(meters.timer("routeshare_payment_capture_latency_seconds"));
    if (captured[0]) {
      meters.counter("routeshare_payment_captures_total", "result", "CAPTURED").increment();
      return CaptureOutcome.captured(bookingId, amount);
    }
    if (failure[0] == null) {
      // The attempt was already recorded under this key: a concurrent duplicate start reached the
      // gateway first. Exactly one capture happened, which is the whole point.
      return CaptureOutcome.alreadyCaptured(bookingId, amount);
    }
    meters.counter("routeshare_payment_captures_total", "result", "FAILED").increment();
    return CaptureOutcome.failed(bookingId, amount, failure[0]);
  }

  /**
   * Runs a gateway call exactly once, whatever happens to the caller.
   *
   * <p>The attempt row goes in first and its unique key is the guard: a duplicate call finds the
   * row already present and does nothing rather than reaching the provider a second time.
   */
  private void attempt(
      PaymentIntentEntity intent,
      Long bookingId,
      String operation,
      String idempotencyKey,
      BigDecimal amount,
      GatewayCall call,
      java.util.function.Consumer<String> onFailure) {
    if (attempts.findByIdempotencyKey(idempotencyKey).isPresent()) {
      log.info("gateway call already attempted key={}", idempotencyKey);
      return;
    }
    PaymentAttemptEntity row =
        attempts.save(
            PaymentAttemptEntity.start(
                intent.getId(),
                bookingId,
                operation,
                idempotencyKey,
                intent.getProviderReference(),
                amount,
                CURRENCY));
    intent.recordAttempt();
    try {
      String providerReference = call.execute();
      row.setProviderReference(providerReference);
      row.succeeded(clock.instant());
      attempts.save(row);
    } catch (RuntimeException ex) {
      String failureCode =
          ex instanceof PaymentDeclinedException declined ? declined.code() : "GATEWAY_ERROR";
      row.failed(clock.instant(), failureCode);
      attempts.save(row);
      // The provider's own message may name the cardholder's bank or reason; it stays in the log.
      log.error("gateway {} failed key={} code={}", operation, idempotencyKey, failureCode, ex);
      onFailure.accept(failureCode);
    }
  }

  private boolean isExternal(PaymentIntentEntity intent) {
    String reference = intent.getProviderReference();
    return reference != null && !reference.startsWith(CASH_REFERENCE_PREFIX);
  }

  private void notify(long bookingId, String type, String title, String body) {
    try {
      bookings
          .findPassengerAppUserIdForBooking(bookingId)
          .ifPresent(
              appUserId ->
                  notifications.notifyUser(
                      appUserId,
                      type,
                      title,
                      body,
                      Map.of("bookingId", String.valueOf(bookingId))));
    } catch (RuntimeException ex) {
      log.warn("payment notification failed bookingId={}", bookingId, ex);
    }
  }

  private String safe(String reason) {
    return reason == null || reason.isBlank() ? "UNSPECIFIED" : reason;
  }

  @FunctionalInterface
  private interface GatewayCall {
    /**
     * @return the provider reference the call produced or confirmed
     */
    String execute();
  }

  /** A refusal from the provider, as a safe code. The provider's wording never leaves the log. */
  static class PaymentDeclinedException extends RuntimeException {
    private final String code;

    PaymentDeclinedException(String code) {
      super(code);
      this.code = code;
    }

    String code() {
      return code;
    }
  }
}
