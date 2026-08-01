package com.routeshare.pricing.service.impl;

import com.routeshare.pricing.dto.request.RouteFareEstimateRequest;
import com.routeshare.pricing.dto.response.FareQuoteResponse;
import com.routeshare.pricing.dto.response.RouteFareResponse;
import com.routeshare.pricing.facade.PricingFacade;
import com.routeshare.pricing.service.PricingQuoteService;
import com.routeshare.routing.facade.RoutingFacade;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PricingQuoteServiceImpl implements PricingQuoteService {
  private final RoutingFacade routing;
  private final PricingFacade pricing;

  @Override
  @Transactional(readOnly = true)
  public RouteFareResponse estimateByRoute(RouteFareEstimateRequest req) {
    double pickup = Math.min(req.pickupRouteFraction(), req.dropoffRouteFraction());
    double dropoff = Math.max(req.pickupRouteFraction(), req.dropoffRouteFraction());
    var trip =
        routing
            .findPriceableTrip(req.routeOccurrenceId())
            .orElseThrow(() -> new NoSuchElementException("Trip not found"));

    long onRouteMeters = Math.round(trip.routeLengthMeters() * (dropoff - pickup));
    BigDecimal matchPercent =
        BigDecimal.valueOf((dropoff - pickup) * 100).setScale(2, RoundingMode.HALF_UP);
    int seats = Math.max(1, req.seats());

    var quote =
        pricing.quoteForMatch(
            req.routeOccurrenceId(),
            trip.vehicleId(),
            BigDecimal.valueOf(onRouteMeters),
            matchPercent,
            seats);
    // A passenger-facing estimate: the driver's cut is withheld.
    return new RouteFareResponse(
        req.routeOccurrenceId(), onRouteMeters, FareQuoteResponse.forPassenger(quote));
  }
}
