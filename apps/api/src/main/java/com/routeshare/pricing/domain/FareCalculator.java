package com.routeshare.pricing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FareCalculator {
  private final BigDecimal baseFare;
  private final BigDecimal perKm;
  private final BigDecimal platformFeeRate;

  public FareCalculator(BigDecimal baseFare, BigDecimal perKm, BigDecimal platformFeeRate) {
    this.baseFare = baseFare;
    this.perKm = perKm;
    this.platformFeeRate = platformFeeRate;
  }

  public static FareCalculator defaultSriLankaCalculator() {
    return new FareCalculator(
        new BigDecimal("250.00"), new BigDecimal("90.00"), new BigDecimal("0.10"));
  }

  public FareBreakdown estimate(long distanceMeters) {
    if (distanceMeters < 0) {
      throw new IllegalArgumentException("distance must not be negative");
    }
    BigDecimal km =
        BigDecimal.valueOf(distanceMeters)
            .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
    BigDecimal distanceFare = perKm.multiply(km).setScale(2, RoundingMode.HALF_UP);
    BigDecimal subtotal = baseFare.add(distanceFare);
    BigDecimal fee = subtotal.multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
    return new FareBreakdown(
        baseFare.setScale(2, RoundingMode.HALF_UP),
        distanceFare,
        fee,
        subtotal.add(fee).setScale(2, RoundingMode.HALF_UP),
        "LKR");
  }
}
