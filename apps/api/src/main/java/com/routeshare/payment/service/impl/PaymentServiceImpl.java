package com.routeshare.payment.service.impl;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.common.ratelimit.RateLimitProperties;
import com.routeshare.common.ratelimit.RateLimiter;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.payment.dto.request.CashCollectionRequest;
import com.routeshare.payment.dto.request.FareAdjustmentRequest;
import com.routeshare.payment.dto.request.PaymentIntentRequest;
import com.routeshare.payment.dto.request.PaymentLifecycleRequest;
import com.routeshare.payment.dto.response.ReceiptResponse;
import com.routeshare.payment.gateway.PaymentGatewayPort;
import com.routeshare.payment.gateway.config.CommissionProperties;
import com.routeshare.payment.repository.FareLedgerRepository;
import com.routeshare.payment.repository.PaymentIntentRepository;
import com.routeshare.payment.repository.PaymentIntentRepository.PaymentIntentView;
import com.routeshare.payment.repository.PaymentMethodRepository;
import com.routeshare.payment.service.PaymentService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
  static final String DEFAULT_CURRENCY = "LKR";
  static final String BOOKING_FARE_ESTIMATE = "BOOKING_FARE_ESTIMATE";
  static final String PAYMENT_CAPTURED = "PAYMENT_CAPTURED";
  static final String PAYMENT_VOIDED = "PAYMENT_VOIDED";
  static final String PAYMENT_REFUNDED = "PAYMENT_REFUNDED";
  static final String CASH_COLLECTED = "CASH_COLLECTED";
  static final String FARE_ADJUSTMENT_REQUESTED = "FARE_ADJUSTMENT_REQUESTED";
  static final String PLATFORM_COMMISSION = "PLATFORM_COMMISSION";
  static final String DRIVER_EARNING = "DRIVER_EARNING";
  static final String FARE_FINALIZED = "FARE_FINALIZED";

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final BookingFacade bookingFacade;
  private final PaymentIntentRepository paymentIntents;
  private final FareLedgerRepository fareLedger;
  private final PaymentGatewayPort gateway;
  private final CommissionProperties commission;
  private final PaymentMethodRepository paymentMethods;
  private final RateLimiter rateLimiter;
  private final RateLimitProperties rateLimits;

  @Override
  @Transactional
  public Map<String, Object> createIntent(PaymentIntentRequest req) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    rateLimiter.check(
        "payment-intent",
        String.valueOf(app.appUserId()),
        rateLimits.paymentIntentPerMinute(),
        java.time.Duration.ofMinutes(1));
    var amount =
        bookingFacade
            .findFareEstimateForPassengerBooking(req.bookingId(), app.appUserId())
            .orElseThrow(
                () -> new AccessDeniedException("Booking does not belong to current user"));
    if (amount.signum() <= 0) {
      throw new IllegalStateException("Booking fare estimate must be positive before payment");
    }

    fareLedger.recordEstimateIfAbsent(
        req.bookingId(), amount, DEFAULT_CURRENCY, BOOKING_FARE_ESTIMATE);

    var existing = paymentIntents.findActiveByBookingId(req.bookingId());
    if (existing.isPresent()) {
      return toResponse(existing.get());
    }

    String providerReference;
    if (req.paymentMethodId() != null) {
      // Card payment: authorize against the stored, tokenized instrument.
      var method =
          paymentMethods
              .findByIdAndAppUserId(req.paymentMethodId(), app.appUserId())
              .orElseThrow(
                  () ->
                      new AccessDeniedException("Payment method does not belong to current user"));
      var auth =
          gateway.authorize(
              new PaymentGatewayPort.AuthorizeCommand(
                  req.bookingId(), amount, DEFAULT_CURRENCY, method.getToken()));
      providerReference = auth.providerReference();
    } else {
      // Cash payment: local reference, no external authorization.
      providerReference = "cash_" + UUID.randomUUID();
    }
    return toResponse(
        paymentIntents.create(req.bookingId(), providerReference, amount, DEFAULT_CURRENCY));
  }

  @Override
  @Transactional
  public Map<String, Object> finalizeBookingFare(long bookingId, BigDecimal finalAmount) {
    if (finalAmount == null || finalAmount.signum() <= 0) {
      throw new IllegalArgumentException("Final fare amount must be positive");
    }
    fareLedger.recordPaymentLifecycleIfAbsent(
        bookingId, FARE_FINALIZED, finalAmount, DEFAULT_CURRENCY);

    boolean captured = false;
    var existing = paymentIntents.findActiveByBookingId(bookingId);
    if (existing.isPresent()) {
      var intent = existing.get();
      boolean external =
          intent.providerReference() != null
              && !intent.providerReference().startsWith("cash_")
              && !intent.providerReference().startsWith("local_");
      if (external && "REQUIRES_CAPTURE".equals(intent.status())) {
        var capturedIntent = transition(intent.paymentIntentId(), "REQUIRES_CAPTURE", "CAPTURED");
        gateway.capture(capturedIntent.providerReference(), finalAmount, DEFAULT_CURRENCY);
        fareLedger.recordPaymentLifecycleIfAbsent(
            bookingId, PAYMENT_CAPTURED, finalAmount, DEFAULT_CURRENCY);
        recordSettlement(bookingId, finalAmount, DEFAULT_CURRENCY);
        captured = true;
      }
    }
    return Map.of(
        "bookingId", bookingId,
        "status", "FARE_FINALIZED",
        "finalAmount", finalAmount,
        "currency", DEFAULT_CURRENCY,
        "captured", captured);
  }

  @Override
  @Transactional
  public Map<String, Object> capture(long paymentIntentId, PaymentLifecycleRequest req) {
    var intent = transition(paymentIntentId, "REQUIRES_CAPTURE", "CAPTURED");
    long bookingId = requireBookingId(intent);
    if (intent.providerReference() != null && !intent.providerReference().startsWith("cash_")) {
      gateway.capture(intent.providerReference(), intent.amount(), intent.currency());
    }
    fareLedger.recordPaymentLifecycleIfAbsent(
        bookingId, PAYMENT_CAPTURED, intent.amount(), intent.currency());
    recordSettlement(bookingId, intent.amount(), intent.currency());
    return toResponse(intent);
  }

  @Override
  @Transactional
  public Map<String, Object> voidIntent(long paymentIntentId, PaymentLifecycleRequest req) {
    var intent = transition(paymentIntentId, "REQUIRES_CAPTURE", "VOIDED");
    if (intent.providerReference() != null && !intent.providerReference().startsWith("cash_")) {
      gateway.voidAuthorization(intent.providerReference());
    }
    fareLedger.recordPaymentLifecycleIfAbsent(
        requireBookingId(intent), PAYMENT_VOIDED, intent.amount(), intent.currency());
    return toResponse(intent);
  }

  @Override
  @Transactional
  public Map<String, Object> refund(long paymentIntentId, PaymentLifecycleRequest req) {
    var intent = transition(paymentIntentId, "CAPTURED", "REFUNDED");
    if (intent.providerReference() != null && !intent.providerReference().startsWith("cash_")) {
      gateway.refund(intent.providerReference(), intent.amount(), intent.currency());
    }
    fareLedger.recordPaymentLifecycleIfAbsent(
        requireBookingId(intent), PAYMENT_REFUNDED, intent.amount().negate(), intent.currency());
    return toResponse(intent);
  }

  @Override
  @Transactional
  public Map<String, Object> recordCashCollected(long bookingId, CashCollectionRequest req) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    var fare =
        bookingFacade
            .findDriverOwnedBookingFare(bookingId, app.appUserId())
            .orElseThrow(
                () -> new AccessDeniedException("Booking does not belong to current driver"));
    if (req.amount().compareTo(fare) > 0) {
      throw new IllegalArgumentException("Cash collection cannot exceed booking fare");
    }
    fareLedger.recordPaymentLifecycleIfAbsent(
        bookingId, CASH_COLLECTED, req.amount(), DEFAULT_CURRENCY);
    recordSettlement(bookingId, req.amount(), DEFAULT_CURRENCY);
    return Map.of(
        "bookingId",
        bookingId,
        "status",
        "CASH_COLLECTED",
        "amount",
        req.amount(),
        "currency",
        DEFAULT_CURRENCY);
  }

  /**
   * Writes audit ledger rows splitting a settled amount into platform commission and net earning.
   */
  private void recordSettlement(long bookingId, BigDecimal gross, String currency) {
    BigDecimal commissionAmount = commission.commissionOn(gross);
    BigDecimal net = gross.subtract(commissionAmount);
    if (commissionAmount.signum() > 0) {
      fareLedger.recordPaymentLifecycleIfAbsent(
          bookingId, PLATFORM_COMMISSION, commissionAmount, currency);
    }
    if (net.signum() > 0) {
      fareLedger.recordPaymentLifecycleIfAbsent(bookingId, DRIVER_EARNING, net, currency);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public ReceiptResponse receipt(long bookingId) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    var fare =
        bookingFacade
            .findFareEstimateForPassengerBooking(bookingId, app.appUserId())
            .orElseThrow(
                () -> new AccessDeniedException("Booking does not belong to current user"));
    List<ReceiptResponse.LineItem> lines =
        fareLedger.findRowsByBookingId(bookingId).stream()
            .map(
                row ->
                    new ReceiptResponse.LineItem(
                        row.getEntryType(), row.getAmount(), row.getCurrency()))
            .toList();
    BigDecimal paid = sum(lines, PAYMENT_CAPTURED);
    BigDecimal refunded = sum(lines, PAYMENT_REFUNDED).abs();
    BigDecimal cash = sum(lines, CASH_COLLECTED);
    BigDecimal balance = fare.subtract(paid).subtract(cash).add(refunded);
    return new ReceiptResponse(
        bookingId, fare, paid, refunded, cash, balance, DEFAULT_CURRENCY, lines);
  }

  @Override
  @Transactional
  public Map<String, Object> requestFareAdjustment(long bookingId, FareAdjustmentRequest req) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    bookingFacade
        .findDriverOwnedBookingFare(bookingId, app.appUserId())
        .orElseThrow(() -> new AccessDeniedException("Booking does not belong to current driver"));
    if (req.amount().signum() == 0) {
      throw new IllegalArgumentException("Fare adjustment amount cannot be zero");
    }
    fareLedger.recordPaymentLifecycleIfAbsent(
        bookingId, FARE_ADJUSTMENT_REQUESTED, req.amount(), DEFAULT_CURRENCY);
    return Map.of(
        "bookingId",
        bookingId,
        "status",
        "FARE_ADJUSTMENT_REQUESTED",
        "amount",
        req.amount(),
        "currency",
        DEFAULT_CURRENCY);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> driverEarningsSummary() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    BigDecimal gross = fareLedger.sumDriverGrossEarnings(app.appUserId());
    if (gross == null) {
      gross = BigDecimal.ZERO;
    }
    BigDecimal commissionAmount = commission.commissionOn(gross);
    BigDecimal net = gross.subtract(commissionAmount);
    return Map.of(
        "currency", DEFAULT_CURRENCY,
        "grossEarnings", gross,
        "platformCommission", commissionAmount,
        "settlementBalance", net,
        "totalEarnings", net);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Map<String, Object>> driverEarningsTransactions() {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    return fareLedger.findDriverLedgerRows(app.appUserId()).stream()
        .map(this::ledgerRowToMap)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Map<String, Object>> adminPayments() {
    return paymentIntents.findAdminPayments().stream().map(this::paymentRowToMap).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> adminPaymentDetail(long paymentIntentId) {
    return paymentIntents
        .findAdminPayment(paymentIntentId)
        .map(this::paymentRowToMap)
        .orElseThrow(() -> new java.util.NoSuchElementException("Payment intent not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Map<String, Object>> adminPaymentEvents(long paymentIntentId) {
    var payment = paymentIntents.findAdminPayment(paymentIntentId).orElseThrow();
    return fareLedger.findRowsByBookingId(payment.getBookingId()).stream()
        .map(
            row ->
                Map.<String, Object>of(
                    "bookingId", payment.getBookingId(),
                    "entryType", row.getEntryType(),
                    "amount", row.getAmount(),
                    "currency", row.getCurrency()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Map<String, Object>> adminCashCollections() {
    return fareLedger.findRowsByTypes(java.util.List.of(CASH_COLLECTED)).stream()
        .map(this::ledgerRowToMap)
        .toList();
  }

  private Map<String, Object> paymentRowToMap(PaymentIntentRepository.PaymentAdminRow row) {
    return Map.of(
        "paymentIntentId", row.getPaymentIntentId(),
        "bookingId", row.getBookingId(),
        "provider", row.getProvider(),
        "providerReference", row.getProviderReference(),
        "status", row.getStatus(),
        "amount", row.getAmount(),
        "currency", row.getCurrency());
  }

  private Map<String, Object> ledgerRowToMap(FareLedgerRepository.FareLedgerAdminRow row) {
    return Map.of(
        "bookingId", row.getBookingId(),
        "entryType", row.getEntryType(),
        "amount", row.getAmount(),
        "currency", row.getCurrency(),
        "createdAt", row.getCreatedAt());
  }

  private PaymentIntentView transition(long paymentIntentId, String fromStatus, String toStatus) {
    return paymentIntents
        .transitionStatus(paymentIntentId, fromStatus, toStatus)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Invalid payment transition from " + fromStatus + " to " + toStatus));
  }

  private long requireBookingId(PaymentIntentView intent) {
    if (intent.bookingId() == null) {
      throw new IllegalStateException("Payment intent is missing booking id");
    }
    return intent.bookingId();
  }

  private BigDecimal sum(List<ReceiptResponse.LineItem> lines, String type) {
    return lines.stream()
        .filter(line -> type.equals(line.type()))
        .map(ReceiptResponse.LineItem::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private Map<String, Object> toResponse(PaymentIntentView intent) {
    return Map.of(
        "paymentIntentId", intent.paymentIntentId() == null ? 0L : intent.paymentIntentId(),
        "bookingId", intent.bookingId() == null ? 0L : intent.bookingId(),
        "provider", intent.provider(),
        "providerReference", intent.providerReference(),
        "status", intent.status(),
        "amount", intent.amount(),
        "currency", intent.currency());
  }
}
