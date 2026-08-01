package com.routeshare.vehicle.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Where a chosen rate sits in its band, with the copy that explains the trade.
 *
 * <p>A rate inside the band is a trade, not a free win: charge the top and you earn more per km but
 * sit below cheaper cars in results. Derived server-side so the driver's screen (D39) and the
 * passenger's explanation (P07) can never tell different stories about the same number.
 *
 * <p>The ranking text describes how results are ordered today. It is copy, not a promise of a
 * ranking multiplier — the server applies no such thing.
 */
public enum RatePosition {
  MIN("Bottom of your band", "Shown above almost every car on your route", "Highest"),
  MID("Middle", "Shown in line with similar cars", "Steady"),
  MAX("Top of your band", "Shown below cheaper cars on the same road", "Lowest");

  private final String label;
  private final String rank;
  private final String demand;

  RatePosition(String label, String rank, String demand) {
    this.label = label;
    this.rank = rank;
    this.demand = demand;
  }

  public String label() {
    return label;
  }

  public String rank() {
    return rank;
  }

  public String demand() {
    return demand;
  }

  /**
   * Bottom third, middle, top third. A zero-width band (min == max) is MID: there is no position to
   * take when there is no room to move.
   */
  public static RatePosition of(BigDecimal rate, BigDecimal min, BigDecimal max) {
    if (rate == null || min == null || max == null) {
      return MID;
    }
    BigDecimal span = max.subtract(min);
    if (span.signum() <= 0) {
      return MID;
    }
    BigDecimal ratio =
        rate.subtract(min).divide(span, 4, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
    if (ratio.compareTo(new BigDecimal("0.3333")) <= 0) {
      return MIN;
    }
    return ratio.compareTo(new BigDecimal("0.6667")) >= 0 ? MAX : MID;
  }
}
