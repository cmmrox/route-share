package com.routeshare.routing.domain;

/**
 * P04's three orderings, applied in the database because results page.
 *
 * <p>Every one of them breaks ties on the occurrence id. Without a total order, two pages of the
 * same search can return the same trip twice and omit another — and a rider scrolling past a seat
 * that silently vanished has no way to find it again.
 */
public enum RideSearchSort {
  /** The default: most of her trip on his road first, cheapest of those first. */
  BEST_MATCH,
  /** Price first, then how well it matches. */
  CHEAPEST,
  /** Leaves soonest. */
  SOONEST;

  public static RideSearchSort of(String value) {
    if (value == null || value.isBlank()) {
      return BEST_MATCH;
    }
    try {
      return RideSearchSort.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException unknown) {
      throw new IllegalArgumentException(
          "sort must be one of BEST_MATCH, CHEAPEST or SOONEST", unknown);
    }
  }
}
