package com.routeshare.pricing.dto.response;

import com.routeshare.pricing.domain.FareQuote;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Every line the fare screens draw, so no client ever does arithmetic on money.
 *
 * <p>{@code commissionAmount} and {@code driverNet} are driver-facing only. Passenger surfaces send
 * {@link #forPassenger(FareQuote)}, which omits them — what a driver keeps is not a rider's
 * business, and a field that is merely "not displayed" is a field that leaks.
 */
public record FareQuoteResponse(
    String currency,
    BigDecimal onRouteDistanceMeters,
    BigDecimal onRouteDistanceKm,
    BigDecimal ratePerKm,
    int seats,
    BigDecimal grossFare,
    BigDecimal matchPercent,
    String matchTier,
    BigDecimal discountPercent,
    BigDecimal discountAmount,
    BigDecimal passengerPays,
    BigDecimal commissionPercent,
    BigDecimal commissionAmount,
    BigDecimal driverNet,
    boolean minFareApplied,
    Instant quotedAt,
    String policyVersion) {

  /** Full quote, for driver and admin surfaces. */
  public static FareQuoteResponse forDriver(FareQuote quote) {
    return of(quote, true);
  }

  /** The same fare with the driver's cut withheld. */
  public static FareQuoteResponse forPassenger(FareQuote quote) {
    return of(quote, false);
  }

  private static FareQuoteResponse of(FareQuote q, boolean includeDriverFigures) {
    return new FareQuoteResponse(
        q.currency(),
        q.onRouteDistanceMeters(),
        q.onRouteDistanceKm(),
        q.ratePerKm(),
        q.seats(),
        q.grossFare(),
        q.matchPercent(),
        q.matchTier().name(),
        q.discountPercent(),
        q.discountAmount(),
        q.passengerPays(),
        includeDriverFigures ? q.commissionPercent() : null,
        includeDriverFigures ? q.commissionAmount() : null,
        includeDriverFigures ? q.driverNet() : null,
        q.minFareApplied(),
        q.quotedAt(),
        q.policyVersion());
  }
}
