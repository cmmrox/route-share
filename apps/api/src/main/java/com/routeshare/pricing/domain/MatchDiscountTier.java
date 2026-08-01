package com.routeshare.pricing.domain;

import java.math.BigDecimal;

/**
 * The route-match discount, banded to the tiers riders already see in search.
 *
 * <p>A rider who overlaps most of a driver's route is cheap for him to carry, so she pays less per
 * km than one he has to detour for. The bands are the product's, not a formula's: a smooth function
 * would be impossible to state on a screen, and "8% because you share 78% of his road" is something
 * a rider can check.
 *
 * <p>The thresholds and percentages are policy values, never constants here — the tier only decides
 * <em>which</em> value applies.
 */
public enum MatchDiscountTier {
  /** 95% overlap or more. */
  HIGH,
  /** 75% or more. */
  MID,
  /** 45% or more. */
  LOW,
  /** Below 45% — the driver is detouring for most of this rider's trip. */
  BASE;

  public static MatchDiscountTier of(
      BigDecimal matchPercent, BigDecimal high, BigDecimal mid, BigDecimal low) {
    if (matchPercent == null) {
      return BASE;
    }
    if (matchPercent.compareTo(high) >= 0) {
      return HIGH;
    }
    if (matchPercent.compareTo(mid) >= 0) {
      return MID;
    }
    return matchPercent.compareTo(low) >= 0 ? LOW : BASE;
  }
}
