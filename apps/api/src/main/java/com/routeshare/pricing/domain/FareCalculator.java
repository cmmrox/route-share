package com.routeshare.pricing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FareCalculator {
  private final BigDecimal baseFare;
  private final BigDecimal perKm;
  private final BigDecimal perMin;
  private final BigDecimal platformFeeRate;

  /** Backward-compatible constructor (no time component). */
  public FareCalculator(BigDecimal baseFare, BigDecimal perKm, BigDecimal platformFeeRate) {
    this(baseFare, perKm, BigDecimal.ZERO, platformFeeRate);
  }

  public FareCalculator(
      BigDecimal baseFare, BigDecimal perKm, BigDecimal perMin, BigDecimal platformFeeRate) {
    this.baseFare = baseFare;
    this.perKm = perKm;
    this.perMin = perMin == null ? BigDecimal.ZERO : perMin;
    this.platformFeeRate = platformFeeRate;
  }

  public static FareCalculator defaultSriLankaCalculator() {
    return new FareCalculator(
        new BigDecimal("250.00"),
        new BigDecimal("90.00"),
        new BigDecimal("5.00"),
        new BigDecimal("0.10"));
  }

  public FareBreakdown estimate(long distanceMeters) {
    return estimate(distanceMeters, 0);
  }

  public FareBreakdown estimate(long distanceMeters, long durationSeconds) {
    if (distanceMeters < 0) {
      throw new IllegalArgumentException("distance must not be negative");
    }
    if (durationSeconds < 0) {
      throw new IllegalArgumentException("duration must not be negative");
    }
    BigDecimal km =
        BigDecimal.valueOf(distanceMeters)
            .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
    BigDecimal distanceFare = perKm.multiply(km).setScale(2, RoundingMode.HALF_UP);
    BigDecimal minutes =
        BigDecimal.valueOf(durationSeconds).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
    BigDecimal timeFare = perMin.multiply(minutes).setScale(2, RoundingMode.HALF_UP);
    BigDecimal subtotal = baseFare.add(distanceFare).add(timeFare);
    BigDecimal fee = subtotal.multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
    return new FareBreakdown(
        baseFare.setScale(2, RoundingMode.HALF_UP),
        distanceFare,
        timeFare,
        fee,
        subtotal.add(fee).setScale(2, RoundingMode.HALF_UP),
        "LKR");
  }
}
