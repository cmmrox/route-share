package com.routeshare.routing.domain;

import com.routeshare.pricing.domain.MatchDiscountTier;
import java.math.BigDecimal;

/**
 * How much of her own trip a rider shares with the driver's road, as the search screen groups it
 * (P05).
 *
 * <p>Derived from {@link MatchDiscountTier} rather than from thresholds of its own. They are the
 * same three numbers, and a second copy is exactly how a rider ends up seeing "Full route" beside
 * an 8% discount — the tier she reads and the discount she is charged would be answering to
 * different constants. One thresholds table, two consumers.
 *
 * <p>This is a presentation of the discount band, not a parallel concept, which is why the mapping
 * is total and has no default arm.
 */
public enum MatchTier {
  /** 95% or more of her trip is on his road. */
  FULL_ROUTE("Full route"),
  /** 75% or more. */
  MOST_OF_ROUTE("Most of route"),
  /** 45% or more. */
  PART_OF_ROUTE("Part of route"),
  /** Below 45% — he is detouring for most of her trip. */
  SHORT_HOP("Short hop");

  private final String label;

  MatchTier(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static MatchTier of(MatchDiscountTier discountTier) {
    return switch (discountTier) {
      case HIGH -> FULL_ROUTE;
      case MID -> MOST_OF_ROUTE;
      case LOW -> PART_OF_ROUTE;
      case BASE -> SHORT_HOP;
    };
  }

  /** Convenience for callers that hold the percentage and the same three thresholds. */
  public static MatchTier of(
      BigDecimal matchPercent, BigDecimal high, BigDecimal mid, BigDecimal low) {
    return of(MatchDiscountTier.of(matchPercent, high, mid, low));
  }
}
