package com.routeshare.pricing.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A priced ride, in full, with every figure the screens draw.
 *
 * <p>Two invariants hold for every quote ever produced, and are asserted by tests and by database
 * CHECK constraints rather than trusted:
 *
 * <ol>
 *   <li>{@code driverNet + commissionAmount == passengerPays} — the commission comes <b>out of</b>
 *       the fare, never on top of it. The passenger sees one price; the driver sees the same price
 *       and what they keep.
 *   <li>{@code passengerPays == grossFare - discountAmount}.
 * </ol>
 */
public record FareQuote(
    String currency,
    BigDecimal onRouteDistanceMeters,
    BigDecimal onRouteDistanceKm,
    BigDecimal ratePerKm,
    int seats,
    BigDecimal grossFare,
    BigDecimal matchPercent,
    MatchDiscountTier matchTier,
    BigDecimal discountPercent,
    BigDecimal discountAmount,
    BigDecimal passengerPays,
    BigDecimal commissionPercent,
    BigDecimal commissionAmount,
    BigDecimal driverNet,
    boolean minFareApplied,
    Instant quotedAt,
    String policyVersion) {}
