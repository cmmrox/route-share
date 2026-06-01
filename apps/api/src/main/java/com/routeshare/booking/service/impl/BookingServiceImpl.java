package com.routeshare.booking.service.impl;

import com.routeshare.booking.dto.request.BookingRequest;
import com.routeshare.booking.repository.BookingRepository;
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
  private final CurrentUserProvider current;
  private final IdentityFacade identityFacade;
  private final BookingRepository bookings;
  private final RoutingFacade routingFacade;
  private final FareCalculator fareCalculator = FareCalculator.defaultSriLankaCalculator();

  @Transactional
  public Map<String, Object> book(BookingRequest req) {
    var app = identityFacade.upsertFromToken(current.requireCurrentUser());
    double routeLengthMeters =
        routingFacade
            .reserveSeatsAndReturnRouteLength(req.routePlanId(), req.seats())
            .orElseThrow(
                () -> new IllegalStateException("Insufficient seats or route unavailable"));
    BigDecimal fareEstimate =
        fareCalculator
            .estimate(Math.round(routeLengthMeters))
            .totalFare()
            .multiply(BigDecimal.valueOf(req.seats()));
    long bookingId = bookings.create(app.appUserId(), req, fareEstimate);
    return Map.of("bookingId", bookingId, "status", "CONFIRMED", "fareEstimate", fareEstimate);
  }
}
