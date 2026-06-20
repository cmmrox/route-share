package com.routeshare.booking.service.impl;

import com.routeshare.booking.dto.request.EarlyDropOffRequest;
import com.routeshare.booking.dto.response.EarlyDropOffResponse;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.service.EarlyDropOffService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.payment.service.PaymentService;
import com.routeshare.pricing.domain.FareCalculator;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
  private final FareCalculator fareCalculator = FareCalculator.defaultSriLankaCalculator();

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

    int seats = ctx.getSeats() == null ? 1 : Math.max(1, ctx.getSeats());
    BigDecimal finalFare =
        fareCalculator
            .estimate(traveledMeters)
            .totalFare()
            .multiply(BigDecimal.valueOf(seats))
            .setScale(2, RoundingMode.HALF_UP);

    bookings.updateDropoffFraction(bookingId, BigDecimal.valueOf(exit));
    var result = payments.finalizeBookingFare(bookingId, finalFare);
    boolean captured = Boolean.TRUE.equals(result.get("captured"));

    return new EarlyDropOffResponse(
        bookingId, traveledMeters, finalFare, "LKR", captured, "FARE_FINALIZED");
  }
}
