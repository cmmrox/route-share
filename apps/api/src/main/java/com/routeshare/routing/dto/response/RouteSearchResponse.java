package com.routeshare.routing.dto.response;

import com.routeshare.pricing.dto.response.FareQuoteResponse;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * One card on P04, complete.
 *
 * <p>The rule for this record is that the client does no arithmetic. Every figure a rider reads —
 * the rate, the price, the discount, how far the driver starts from her, which tier this match is —
 * is computed once, on the server, from the same inputs that will price the booking. A client that
 * recomputes any of them will eventually disagree with the checkout, and the rider will be right
 * while the app is wrong.
 *
 * <p>{@code startsKmAway} is a <em>distance</em> and {@code originLabel} is a place name. The
 * driver's actual origin coordinate is never emitted: for most drivers it is their home.
 */
public record RouteSearchResponse(
    long routePlanId,
    long routeOccurrenceId,
    String originLabel,
    String destinationLabel,
    Instant departureTime,
    int availableSeats,
    double routeLengthMeters,
    double pickupRouteFraction,
    double dropoffRouteFraction,
    double pickupDistanceMeters,
    double dropoffDistanceMeters,
    double overlapDistanceMeters,
    double overlapPercent,
    double score,
    String explanation,
    long matchedDistanceMeters,
    /** Mirrors {@code fare.passengerPays}; kept so existing readers do not break. */
    BigDecimal estimatedFare,
    String currency,
    /** The full v2 quote, with the driver's cut withheld — this is a passenger surface. */
    FareQuoteResponse fare,
    String driverName,
    String vehicleMake,
    String vehicleModel,
    String vehicleRegistration,
    Integer vehicleSeatCount,

    // ── slice 09 ─────────────────────────────────────────────────────────────────────────────────

    /**
     * How far the driver's trip starts from the rider — the number the radius filtered on (P04).
     */
    double startsKmAway,
    /** P05 groups on this. Derived from the discount band, never from thresholds of its own. */
    String matchTier,
    String matchTierLabel,
    /** A sentence a rider can check, rather than a percentage she has to interpret. */
    String overlapSummary,
    String vehicleColour,
    String vehicleClassKey,
    /** P07 explains the rate from the driver's chosen point inside his admin-set band. */
    BigDecimal ratePerKm,
    BigDecimal classBandMinRate,
    BigDecimal classBandMaxRate,
    String approvalMode,
    boolean womenOnly,
    boolean verifiedRidersOnly) {}
