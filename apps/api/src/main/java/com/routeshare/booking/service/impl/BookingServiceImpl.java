package com.routeshare.booking.service.impl;

import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.repository.BookingRepository;
import com.routeshare.booking.repository.BookingStatusHistoryRepository;
import com.routeshare.booking.service.BookingService;
import com.routeshare.common.security.CurrentUserProvider;
import com.routeshare.identity.facade.IdentityFacade;
import com.routeshare.pricing.domain.FareCalculator;
import com.routeshare.routing.facade.RoutingFacade;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
  private static final String CONFIRMED = "CONFIRMED";
  private static final String INITIAL_CONFIRMATION_REASON =
      "Booking confirmed after occurrence seat reservation";

  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final BookingRepository bookings;
  private final BookingStatusHistoryRepository statusHistory;
  private final RoutingFacade routingFacade;
  private final FareCalculator fareCalculator = FareCalculator.defaultSriLankaCalculator();

  @Transactional
  public Map<String, Object> book(BookingRequest req) {
    validateMatchedFractions(req);
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    var reservation =
        routingFacade
            .reserveSeatsAndReturnRouteLength(req.routeOccurrenceId(), req.seats())
            .orElseThrow(
                () -> new IllegalStateException("Insufficient seats or route unavailable"));
    long matchedDistanceMeters =
        Math.round(
            reservation.routeLengthMeters()
                * (req.dropoffRouteFraction() - req.pickupRouteFraction()));
    BigDecimal fareEstimate =
        fareCalculator
            .estimate(matchedDistanceMeters)
            .totalFare()
            .multiply(BigDecimal.valueOf(req.seats()));
    long bookingId = bookings.create(app.appUserId(), req, reservation.routePlanId(), fareEstimate);
    statusHistory.recordInitialStatus(
        bookingId, CONFIRMED, app.appUserId(), INITIAL_CONFIRMATION_REASON);
    return Map.of(
        "bookingId",
        bookingId,
        "status",
        CONFIRMED,
        "routeOccurrenceId",
        reservation.routeOccurrenceId(),
        "fareEstimate",
        fareEstimate);
  }

  private void validateMatchedFractions(BookingRequest req) {
    if (req.pickupRouteFraction() >= req.dropoffRouteFraction()) {
      throw new IllegalArgumentException("Pickup must be before drop-off on the matched route");
    }
  }
}
