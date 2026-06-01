package com.routeshare.payment.service.impl;

import com.routeshare.booking.facade.BookingFacade;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.payment.dto.request.PaymentIntentRequest;
import com.routeshare.payment.repository.FareLedgerRepository;
import com.routeshare.payment.repository.PaymentIntentRepository;
import com.routeshare.payment.service.PaymentService;
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

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final BookingFacade bookingFacade;
  private final PaymentIntentRepository paymentIntents;
  private final FareLedgerRepository fareLedger;

  @Transactional
  public Map<String, Object> createIntent(PaymentIntentRequest req) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
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
    var intent =
        paymentIntents
            .findActiveByBookingId(req.bookingId())
            .orElseGet(
                () ->
                    paymentIntents.create(
                        req.bookingId(), "mock_" + UUID.randomUUID(), amount, DEFAULT_CURRENCY));
    return Map.of(
        "provider", intent.provider(),
        "providerReference", intent.providerReference(),
        "status", intent.status(),
        "amount", intent.amount(),
        "currency", intent.currency());
  }
}
