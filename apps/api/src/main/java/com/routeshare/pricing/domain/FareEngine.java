package com.routeshare.pricing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * The fare, computed. Pure: every input is a parameter, so the arithmetic can be tested against the
 * prototype's fixtures without a database, a clock or a request.
 *
 * <pre>
 *   grossFare      = round(onRouteKm × ratePerKm) × seats
 *   discountAmount = round(grossFare × discountPct / 100)
 *   passengerPays  = grossFare − discountAmount        (floored at the minimum fare)
 *   commission     = round(passengerPays × commissionPct / 100)
 *   driverNet      = passengerPays − commission
 * </pre>
 *
 * <p>There is no base fare and no time component. A rider pays for the distance they actually ride
 * on a road the driver was taking anyway; charging for time would charge them for his traffic.
 *
 * <p><b>driverNet is computed by subtraction, never by its own multiplication.</b> Rounding two
 * percentages independently and hoping they add back to the whole is how a ledger drifts by a rupee
 * per trip; subtraction makes the invariant true by construction.
 */
public final class FareEngine {
  private static final int MONEY_SCALE = 2;
  private static final BigDecimal METERS_PER_KM = BigDecimal.valueOf(1000);
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private FareEngine() {}

  /**
   * @param onRouteMeters the matched substring of the driver's route — server-derived geometry,
   *     never a distance the client sent
   * @param minFare floor below which a very short overlap must not price; may be null
   */
  public static FareQuote quote(
      String currency,
      BigDecimal onRouteMeters,
      BigDecimal ratePerKm,
      int seats,
      BigDecimal matchPercent,
      MatchDiscountTier tier,
      BigDecimal discountPercent,
      BigDecimal commissionPercent,
      BigDecimal minFare,
      Instant quotedAt,
      String policyVersion) {
    if (onRouteMeters == null || onRouteMeters.signum() < 0) {
      throw new IllegalArgumentException("on-route distance must not be negative");
    }
    if (ratePerKm == null || ratePerKm.signum() <= 0) {
      throw new IllegalArgumentException("rate per km must be positive");
    }
    if (seats < 1) {
      throw new IllegalArgumentException("seats must be at least 1");
    }

    BigDecimal km = onRouteMeters.divide(METERS_PER_KM, 4, RoundingMode.HALF_UP);
    // Rounded per seat and then multiplied, so two seats always cost exactly twice one seat — a
    // rider who books two seats and a friend who books one each must never be charged differently.
    BigDecimal perSeat = money(km.multiply(ratePerKm));
    BigDecimal grossFare = money(perSeat.multiply(BigDecimal.valueOf(seats)));

    BigDecimal discountAmount = money(grossFare.multiply(discountPercent).divide(HUNDRED));
    BigDecimal passengerPays = grossFare.subtract(discountAmount);

    boolean minFareApplied = false;
    if (minFare != null && minFare.signum() > 0 && passengerPays.compareTo(minFare) < 0) {
      passengerPays = money(minFare);
      // The floor lifts what the passenger pays, so the discount line must still describe the gap
      // between gross and paid or the receipt would not add up on screen.
      discountAmount = grossFare.subtract(passengerPays).max(BigDecimal.ZERO);
      if (passengerPays.compareTo(grossFare) > 0) {
        grossFare = passengerPays;
        discountAmount = BigDecimal.ZERO;
      }
      minFareApplied = true;
    }

    BigDecimal commissionAmount = money(passengerPays.multiply(commissionPercent).divide(HUNDRED));
    BigDecimal driverNet = passengerPays.subtract(commissionAmount);

    return new FareQuote(
        currency,
        money(onRouteMeters),
        km,
        ratePerKm,
        seats,
        grossFare,
        matchPercent == null ? BigDecimal.ZERO : matchPercent,
        tier,
        discountPercent,
        discountAmount,
        passengerPays,
        commissionPercent,
        commissionAmount,
        driverNet,
        minFareApplied,
        quotedAt,
        policyVersion);
  }

  /**
   * Money is rounded to whole rupees and then carried at scale 2.
   *
   * <p>Sri Lankan fares are quoted in whole rupees — the prototype rounds every figure it shows,
   * and a receipt reading "LKR 266.80" would be a number no one can hand over. Rounding at each
   * step and deriving {@code driverNet} by subtraction keeps the on-screen arithmetic addable: 290
   * gross, 23 discount, 267 paid, 27 commission, 240 net, exactly as the fixtures state.
   */
  private static BigDecimal money(BigDecimal value) {
    return value.setScale(0, RoundingMode.HALF_UP).setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
  }
}
