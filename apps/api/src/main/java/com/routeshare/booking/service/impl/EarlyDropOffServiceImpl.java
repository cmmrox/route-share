package com.routeshare.booking.service.impl;

import com.routeshare.booking.dto.request.EarlyDropOffRequest;
import com.routeshare.booking.dto.response.EarlyDropOffResponse;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.service.EarlyDropOffService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.payment.service.PaymentService;
import com.routeshare.pricing.facade.PricingFacade;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EarlyDropOffServiceImpl implements EarlyDropOffService {
  private static final String CONFIRMED = "CONFIRMED";

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final BookingRepository bookings;
  private final PaymentService payments;
  private final PricingFacade pricing;
  private final com.routeshare.payment.facade.PaymentFacade paymentFacade;
  private final com.routeshare.reliability.facade.ReliabilityFacade reliability;

  @Override
  @Transactional
  public EarlyDropOffResponse finalizeEarlyDropOff(long bookingId, EarlyDropOffRequest req) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    var ctx =
        bookings
            .findEarlyDropOffContext(bookingId, app.appUserId(), req.latitude(), req.longitude())
            .orElseThrow(
                () -> new AccessDeniedException("Booking does not belong to current user"));

    if (!CONFIRMED.equalsIgnoreCase(ctx.getStatus())) {
      throw new IllegalStateException("Early drop-off is only allowed for an active trip");
    }
    if (ctx.getRouteLengthM() == null || ctx.getExitFraction() == null) {
      throw new IllegalStateException("Route geometry is unavailable for this booking");
    }

    double pickup = ctx.getPickupFraction().doubleValue();
    double dropoff = ctx.getDropoffFraction().doubleValue();
    // Clamp the projected exit point between the pickup and the originally-booked drop-off.
    double exit = Math.max(pickup, Math.min(dropoff, ctx.getExitFraction()));
    long traveledMeters = Math.round(ctx.getRouteLengthM().doubleValue() * (exit - pickup));

    // Two adjusted drops a calendar month. Spent here, before the reprice, so the decision and the
    // money cannot disagree: either the allowance was taken and the fare moved, or neither.
    boolean adjusted = reliability.consumeEarlyDropAllowance(app.appUserId(), bookingId, null);

    // Repriced on the distance actually travelled, at the rate and tier the rider booked under —
    // but only within the allowance. Beyond it the fare she agreed to stands.
    BigDecimal finalFare =
        adjusted
            ? pricing
                .repriceForActualDistance(bookingId, BigDecimal.valueOf(traveledMeters))
                .passengerPays()
            : ctx.getFareEstimate();

    // The seat is released either way: she is out of the car, and the route she is no longer on
    // should not keep counting as hers.
    bookings.updateDropoffFraction(bookingId, BigDecimal.valueOf(exit));
    // Captures the lower figure if nothing has been taken yet, refunds the difference if it has.
    paymentFacade.settleRepricedFare(bookingId, finalFare);
    var result = payments.finalizeBookingFare(bookingId, finalFare);
    boolean captured = Boolean.TRUE.equals(result.get("captured"));

    var allowance = reliability.earlyDropAllowance(app.appUserId());
    return new EarlyDropOffResponse(
        bookingId,
        traveledMeters,
        finalFare,
        "LKR",
        captured,
        adjusted,
        adjusted ? null : "EARLY_DROP_ALLOWANCE_EXHAUSTED",
        allowance.used(),
        allowance.remaining(),
        "FARE_FINALIZED");
  }
}
